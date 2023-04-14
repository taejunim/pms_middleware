package pms.system.ess;

import pms.communication.device.pcs.PCSClient;
import pms.communication.external.smarthub.EVChargerClient;
import pms.vo.device.PcsVO;
import pms.vo.device.control.ControlRequestVO;
import pms.vo.device.external.EVChargerVO;
import pms.vo.system.EssVO;
import pms.vo.system.PmsVO;

import java.util.List;

public class ESSController {
    private static boolean isMinHolding;
    private static float MIN_HOLDING_SOC; //최소 유지 SoC
    private static float MIN_OPERATION_SOC;   //최소 운전 SoC
    private static float MAX_OPERATION_SOC;   //최대 운전 SoC
    private final PCSClient pcsClient = new PCSClient();
    private final ESSManager essManager = new ESSManager();
    private final EVChargerClient evChargerClient = new EVChargerClient();

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
        int limitPower = new ESSManager().calculateLimitPower();

        if (limitPower < referencePower) {
            String controlValue = String.valueOf(limitPower);

            ControlRequestVO requestVO = ControlUtil.setControlRequestVO("04", "0402", "02", "0200010205", controlValue, null);
            pcsClient.setControlRequest(requestVO);
        }
    }

    public void autoControlPCS(PcsVO pcsVO) {
        String operation = pcsVO.getOperationStatus();
        String operationMode = pcsVO.getOperationModeStatus();
        float averageSoC = new ESSManager().averageSoC();
        boolean isRackOperation = new ESSManager().isRackOperation();

        //System.out.println("평균 SoC : " + averageSoC);
        //System.out.println("Rack 운영 가능 여부 : " + isRackOperation);

        //경고 및 결함 상태가 아닌 경우에 실행
        /*if (pcsVO.getWarningFlag().equals("N") && pcsVO.getFaultFlag().equals("N")) {

        }*/

        //제어 요청이 없는 경우에 실행
        if (!pcsClient.isControlRequest()) {
            EVChargerClient evChargerClient = new EVChargerClient();
            String evChargerRequest = evChargerClient.getEVChargerRequest();

            System.out.println("평균 SoC : " + averageSoC);

            //EV 충전기 요청 확인
            if (evChargerRequest != null) {
                System.out.println("EV 충전기 요청 존재");
                controlByEVCharger(evChargerRequest, operation, operationMode, averageSoC);
            } else {
                System.out.println("EV 충전기 요청 미존재 - SoC 확인");
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
        //PCS 운전 상태 확인
        if (operation.equals("11")) {
            if (pcsVO.getWarningFlag().equals("N") && pcsVO.getFaultFlag().equals("N")) {
                controlAutoRun("03", "0300", socConfigCode); //운전
            }
        } else if (operation.equals("12")) {
            //최소 SoC 유지 운전 상태인지 확인
            if (!isMinHolding) {
                evChargerClient.request();
                List<EVChargerVO> chargers = evChargerClient.getEVChargers("ess-charge");
                int totalChargerCount = evChargerClient.getChargerCount();

                if (chargers.size() == totalChargerCount) {
                    int limitPower = new ESSManager().calculateLimitPower();
                    String controlValue = String.valueOf(limitPower);

                    controlAutoCharge("03", "0301", controlValue, socConfigCode);    //충전 - 최소 SoC
                    isMinHolding = true;
                } else if (chargers.size() < totalChargerCount) {
                    controlAutoCharge("03", "0301", "5", socConfigCode);    //충전 - 최소 SoC
                    isMinHolding = true;

                    System.out.println("EV 충전기 일부 충전 중 ESS 저전력 충전 가능!");
                }
            }
        }
    }

    private void controlByEVCharger(String request, String operation, String operationMode, float averageSoC) {
        if (request != null) {
            if (operation.equals("12")) {
                /*switch (operationMode) {
                    case "0":
                        if (request.equals("allCharging") || request.equals("charging")) {
                            if (averageSoC > MIN_OPERATION_SOC) {
                                controlAutoDischarge("05", "0502"); //ESS 방전
                                System.out.println("[EV 충전기] 제어 요청 - ESS 방전");
                            } else {
                                new EVChargerClient().removeEVChargerRequest();
                            }
                        }
                        break;
                    case "1":
                        switch (request) {
                            case "allCharging":
                                controlAutoStandby("05", "0501");   //대기
                                System.out.println("[EV 충전기] 제어 요청 - ESS 대기 : 모든 EV 충전기 충전 중");
                                break;
                            case "charging":
                                controlAutoCharge("05", "0503", "5", null); //ESS 저전력 충전
                                System.out.println("[EV 충전기] 제어 요청 - ESS 저전력 충전");
                                break;
                            case "standby":
                                if (averageSoC < MAX_OPERATION_SOC) {
                                    int limitPower = new ESSManager().calculateLimitPower();
                                    String controlValue = String.valueOf(limitPower);

                                    controlAutoCharge("04", "0402", controlValue, null);    //전력 변경
                                }

                                new EVChargerClient().removeEVChargerRequest();
                                System.out.println("[EV 충전기] 제어 요청 - 충전 전력 변경 : 모든 EV 충전기 종료");
                                break;
                        }
                        break;
                    case "2":

                        break;
                }*/

                switch (request) {
                    case "allCharging":   //모든 EV 충전기 충전
                        if (operationMode.equals("0")) {
                            if (averageSoC > MIN_OPERATION_SOC) {
                                controlAutoDischarge("05", "0502"); //ESS 방전

                                System.out.println("[EV 충전기] 제어 요청 - ESS 방전");
                            } else {
                                new EVChargerClient().removeEVChargerRequest();
                            }
                        } else if (operationMode.equals("1")) {
                            controlAutoStandby("05", "0501");   //대기

                            System.out.println("[EV 충전기] 제어 요청 - ESS 대기 : 모든 EV 충전기 충전 중");
                        }
                        break;
                    case "charging":  //일부 EV 충전기 충전
                        if (operationMode.equals("0")) {
                            if (averageSoC > MIN_OPERATION_SOC) {
                                controlAutoDischarge("05", "0502"); //ESS 방전

                                System.out.println("[EV 충전기] 제어 요청 - ESS 방전");
                            } else {
                                new EVChargerClient().removeEVChargerRequest();
                            }
                        } else if (operationMode.equals("1")) {
                            controlAutoCharge("05", "0503", "5", null); //ESS 저전력 충전

                            System.out.println("[EV 충전기] 제어 요청 - ESS 저전력 충전");
                        }
                        break;
                    case "standby": //모든 EV 충전기 대기
                        if (operationMode.equals("2")) {
                            controlAutoStop("05", "0504", null);    //EV 충전 종료 - PCS 운전 종료

                            System.out.println("[EV 충전기] 제어 요청 - PCS 운전 종료 : 모든 EV 충전기 종료");
                        } else if (operationMode.equals("1")) {
                            if (averageSoC < MAX_OPERATION_SOC) {
                                int limitPower = new ESSManager().calculateLimitPower();
                                String controlValue = String.valueOf(limitPower);

                                controlAutoCharge("04", "0402", controlValue, null);    //전력 변경
                            }

                            new EVChargerClient().removeEVChargerRequest();

                            System.out.println("[EV 충전기] 제어 요청 - 충전 전력 변경 : 모든 EV 충전기 종료");
                        }
                        break;
                }
            } else if (operation.equals("11")) {
                if (request.equals("allCharging") || request.equals("charging")) {
                    if (averageSoC > MIN_OPERATION_SOC) {
                        controlAutoRun("05", "0500", null);

                        System.out.println("[EV 충전기] 제어 요청 - PCS 운전 기동");
                    } else {
                        new EVChargerClient().removeEVChargerRequest();
                    }
                }
            }
        }
    }
}
