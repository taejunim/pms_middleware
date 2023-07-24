package pms.system.ess;

import pms.communication.device.pcs.PCSClient;
import pms.communication.external.smarthub.EVChargerClientNew;
import pms.vo.device.PcsVO;
import pms.vo.device.control.ControlRequestVO;
import pms.vo.device.external.EVChargerVO;
import pms.vo.system.EssVO;
import pms.vo.system.PmsVO;

import java.util.List;

public class ESSController {
    private static boolean isMinHolding;    //최소 SoC 보유를 위한 충전 진행 여부
    private static float MIN_HOLDING_SOC; //최소 보유 SoC
    private static float MIN_OPERATION_SOC; //최소 운영 SoC
    private static float MAX_OPERATION_SOC; //최대 운영 SoC
    private final PCSClient pcsClient = new PCSClient();
    private final ESSManager essManager = new ESSManager();
    private final EVChargerClientNew evChargerClientNew = new EVChargerClientNew();

    public void setConfig() {
        String essType = PmsVO.ess.getEssType();    //01: 고정형 ESS, 02: 이동형 ESS

        if (essType.equals("01")) {
            EssVO.ConfigVO soc = PmsVO.pcsConfigs.get("03");
            MIN_HOLDING_SOC = soc.getMinSetValue().floatValue();
            MIN_OPERATION_SOC = soc.getMinSetValue().floatValue();
            MAX_OPERATION_SOC = soc.getMaxSetValue().floatValue();
        }
    }

    private void controlAutoRun(String type, String detailType, String referenceCode) {
        ControlRequestVO requestVO = ControlUtil.setControlRequestVO(type, detailType, "02", "0200010201", null, referenceCode);
        pcsClient.setControlRequest(requestVO);
    }

    private void controlAutoStop(String type, String detailType, String referenceCode) {
        ControlRequestVO requestVO = ControlUtil.setControlRequestVO(type, detailType, "02", "0200010202", null, referenceCode);
        pcsClient.setControlRequest(requestVO);
    }

    private void controlAutoStandby(String type, String detailType) {
        ControlRequestVO requestVO = ControlUtil.setControlRequestVO(type, detailType, "02", "0200010204", null, null);
        pcsClient.setControlRequest(requestVO);
    }

    private void controlAutoCharge(String type, String detailType, String controlValue, String referenceCode) {
        ControlRequestVO requestVO = ControlUtil.setControlRequestVO(type, detailType, "02", "0200010205", controlValue, referenceCode);
        pcsClient.setControlRequest(requestVO);
    }

    private void controlAutoDischarge(String type, String detailType) {
        ControlRequestVO requestVO = ControlUtil.setControlRequestVO(type, detailType, "02", "0200010206", null, null);
        pcsClient.setControlRequest(requestVO);
    }

    private void controlPower(int referencePower) {
        int limitPower = essManager.calculateLimitPower();

        if (limitPower < referencePower) {
            String controlValue = String.valueOf(limitPower);

            ControlRequestVO requestVO = ControlUtil.setControlRequestVO("04", "0402", "02", "0200010205", controlValue, null);
            pcsClient.setControlRequest(requestVO);
        }
    }

    public void autoControlPCS(PcsVO pcsVO) {
        String operation = pcsVO.getOperationStatus();
        String operationMode = pcsVO.getOperationModeStatus();
        float averageSoC = essManager.averageSoC();
        boolean isRackOperate = essManager.isRackOperation();

        //System.out.println("평균 SoC : " + averageSoC);
        //System.out.println("Rack 운영 가능 여부 : " + isRackOperate);

        //경고 및 결함 상태가 아닌 경우에 실행
        /*if (pcsVO.getWarningFlag().equals("N") && pcsVO.getFaultFlag().equals("N")) {

        }*/

        if (isRackOperate) {
            //제어 요청이 없는 경우에 실행
            if (!pcsClient.isControlRequest()) {
                String evChargerRequest = evChargerClientNew.getControlRequest();

                //System.out.println("평균 SoC : " + averageSoC);

                //EV 충전기 요청 확인
                if (evChargerRequest != null) {
                    System.out.println("EV 충전기 요청 존재 : " + evChargerRequest);
                    controlByEVCharger(evChargerRequest, operation, operationMode, averageSoC);
                }

                controlBySoC(operation, operationMode, averageSoC, pcsVO);
            }
        }
    }

    private void controlBySoC(String operation, String operationMode, float averageSoC, PcsVO pcsVO) {
        String socConfigCode = PmsVO.pcsConfigs.get("03").getConfigCode();

        switch (operationMode) {
            case "0":   //대기
                //최소 유지 SoC 확인
                if (averageSoC < MIN_HOLDING_SOC) {
                    controlByMinSoC(operation, socConfigCode, pcsVO);
                }
                break;
            case "1":   //충전
                //최소 SoC 유지 운전 상태인지 확인
                if (!isMinHolding) {
                    if (averageSoC >= MAX_OPERATION_SOC) {
                        controlAutoStop("03", "0302", socConfigCode);    //정지 - 최대 SoC
                    } else {
                        controlPower(pcsVO.getReferencePower());    //충전 전력 조절
                    }
                } else {
                    if (averageSoC >= MIN_HOLDING_SOC) {
                        controlAutoStop("03", "0301", socConfigCode);    //정지 - 최소 SoC
                        isMinHolding = false;
                    } else {
                        controlPower(pcsVO.getReferencePower());    //충전 전력 조절
                    }
                }
                break;
            case "2":   //방전
                if (averageSoC <= MIN_OPERATION_SOC) {
                    controlAutoStop("03", "0301", socConfigCode);    //정지 - 최소 SoC
                }
                break;
        }
    }

    private void controlByMinSoC(String operation, String socConfigCode, PcsVO pcsVO) {
        //PCS 운전 상태 확인 (11: 정지, 12: 운전)
        if (operation.equals("11")) {
            if (pcsVO.getWarningFlag().equals("N") && pcsVO.getFaultFlag().equals("N")) {
                controlAutoRun("03", "0300", socConfigCode); //운전
            }
        } else if (operation.equals("12")) {
            //최소 SoC 유지 운전 상태인지 확인
            if (!isMinHolding) {
                evChargerClientNew.request();   //EV 충전기 API 요청
                List<EVChargerVO> standbyChargers = evChargerClientNew.getEVChargers("ess-charge");    //대기 상태의 충전기 목록 - ESS 충전 가능 여부 확인을 위해 대기 상태인 EV 충전기 목록 호출
                int totalChargerCount = evChargerClientNew.getTotalChargerCount();  //총 EV 충전기 개수

                if (standbyChargers.size() == totalChargerCount) {
                    int limitPower = essManager.calculateLimitPower();
                    String controlValue = String.valueOf(limitPower);

                    controlAutoCharge("03", "0301", controlValue, socConfigCode);    //충전 - 최소 SoC
                    isMinHolding = true;
                } else if (standbyChargers.size() < totalChargerCount) {
                    controlAutoCharge("03", "0301", "5", socConfigCode);    //충전 - 최소 SoC
                    isMinHolding = true;

                    System.out.println("EV 충전기 일부 충전 중 ESS 저전력 충전 가능!");
                }
            }
        }
    }

    private void controlByEVCharger(String request, String operation, String operationMode, float averageSoC) {
        //PCS 운전 상태 확인 (11: 정지, 12: 운전)
        if (operation.equals("11")) {
            //if (request.equals("charging") || request.equals("all-charging")) {
            //EV 충전기 충전 준비
            if (request.equals("ready")) {
                //SoC가 최소 운영 SoC보다 큰 경우 실행
                if (averageSoC > MIN_OPERATION_SOC) {
                    controlAutoRun("05", "0500", null); //PCS 운전
                    System.out.println("[EV 충전기] 충전 준비 제어 요청 - PCS 운전 기동");
                } else {
                    //제어 취소 및 초기화
                    evChargerClientNew.resetControlRequest();
                }
            }
        } else if (operation.equals("12")) {
            switch (request) {
                case "ready":
                    if (operationMode.equals("1")) {
                        controlAutoStandby("05", "0590");   //대기
                        System.out.println("[EV 충전기] 제어 요청 - ESS 대기 : EV 충전기 충전 준비");
                    } else if (operationMode.equals("0")) {
                        System.out.println("EV 충전기 충전 준비 상태");
                        evChargerClientNew.resetControlRequest();
                    }
                    break;
                case "charging":
                case "all-charging":
                    switch (operationMode) {
                        case "0":   //ESS 대기 상태
                            if (averageSoC > MIN_OPERATION_SOC) {
                                controlAutoDischarge("05", "0502"); //ESS 방전
                                System.out.println("[EV 충전기] 제어 요청 - ESS 방전");
                            } else {
                                evChargerClientNew.resetControlRequest();
                            }
                            break;
                        case "1":   //ESS 충전 상태
                            if (request.equals("charging")) {
                                controlAutoCharge("05", "0503", "5", null); //ESS 저전력 충전
                                System.out.println("[EV 충전기] 제어 요청 - ESS 저전력 충전");
                            } else {
                                controlAutoStandby("05", "0501");   //대기
                                System.out.println("[EV 충전기] 제어 요청 - ESS 대기 : 모든 EV 충전기 충전 중");
                            }
                            break;
                        case "2":   //ESS 방전 상태
                            break;
                    }
                    break;
                case "end":
                    if (operationMode.equals("1")) {
                        if (averageSoC < MAX_OPERATION_SOC) {
                            int limitPower = essManager.calculateLimitPower();
                            String controlValue = String.valueOf(limitPower);

                            controlAutoCharge("05", "0503", controlValue, null);    //전력 변경
                        }

                        evChargerClientNew.resetControlRequest();
                        System.out.println("[EV 충전기] 제어 요청 - 충전 전력 변경 : 모든 EV 충전기 종료");
                    } else if (operationMode.equals("2")) {
                        controlAutoStop("05", "0504", null);    //EV 충전 종료 - PCS 운전 종료
                        System.out.println("[EV 충전기] 제어 요청 - PCS 운전 종료: 모든 EV 충전기 종료");
                    }
                    break;
                case "cancel":
                    if (operationMode.equals("0")) {
                        controlAutoStop("05", "0591", null);    //EV 충전 취소 - PCS 운전 종료
                        System.out.println("[EV 충전기] 제어 요청 - PCS 운전 종료: EV 충전 취소");
                    }
                    break;
                case "error":
                    if (operationMode.equals("2")) {
                        controlAutoStop("05", "0599", null);    //EV 충전 오류 - PCS 운전 종료
                        System.out.println("[EV 충전기] 제어 요청 - PCS 운전 종료: EV 충전 오류");
                    }
                    break;
            }
        }
    }
}
