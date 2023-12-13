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
    private final PCSClient pcsClient = new PCSClient();
    private final ESSManager essManager = new ESSManager();
    private final String autoControlFlag = PmsVO.ess.getAutoControlFlag();
    private static boolean isHoldingMinSoC;    //최소 SoC 보유를 위한 충전 진행 여부
    private static float MIN_HOLDING_SOC; //최소 보유 SoC
    private static float MIN_OPERATION_SOC; //최소 운영 SoC
    private static float MAX_OPERATION_SOC; //최대 운영 SoC
    private static boolean isChargingEVCharger;
    private static boolean isControllingByCharger;

    private static final float chargePower = 0;
    private static final float dischargePower = 0;
    //private final EVChargerClientNew evChargerClientNew = new EVChargerClientNew();

    /**
     * 환경 설정 정보 설정
     * <p>
     * - 최소 보유 SoC, 최소 및 최대 운영 SoC
     */
    public void setConfig() {
        String essType = PmsVO.ess.getEssType();    //01: 고정형 ESS, 02: 이동형 ESS

        if (essType.equals("01")) {
            EssVO.ConfigVO soc = PmsVO.pcsConfigs.get("03");
            MIN_HOLDING_SOC = soc.getMinSetValue().floatValue();
            MIN_OPERATION_SOC = soc.getMinSetValue().floatValue();
            MAX_OPERATION_SOC = soc.getMaxSetValue().floatValue();
        }
    }

    /**
     * 운전 제어
     *
     * @param type          제어 요청 구분
     * @param detailType    제어 요청 상세 구분
     * @param referenceCode 참조 코드
     */
    private void controlRun(String type, String detailType, String referenceCode) {
        ControlRequestVO requestVO = ControlUtil.setControlRequestVO(type, detailType, "02", "0200010201", null, referenceCode);
        pcsClient.setControlRequest(requestVO);
    }

    /**
     * 운전 정지 제어
     *
     * @param type          제어 요청 구분
     * @param detailType    제어 요청 상세 구분
     * @param referenceCode 참조 코드
     */
    private void controlStop(String type, String detailType, String referenceCode) {
        ControlRequestVO requestVO = ControlUtil.setControlRequestVO(type, detailType, "02", "0200010202", null, referenceCode);
        pcsClient.setControlRequest(requestVO);
    }

    /**
     * 대기 모드 제어
     *
     * @param type       제어 요청 구분
     * @param detailType 제어 요청 상세 구분
     */
    private void controlAutoStandby(String type, String detailType) {
        ControlRequestVO requestVO = ControlUtil.setControlRequestVO(type, detailType, "02", "0200010204", null, null);
        pcsClient.setControlRequest(requestVO);
    }

    /**
     * 충전 모드 제어
     *
     * @param type          제어 요청 구분
     * @param detailType    제어 요청 상세 구분
     * @param controlValue  제어 요청 값(충전 전력)
     * @param referenceCode 참조 코드
     */
    private void controlCharge(String type, String detailType, String controlValue, String referenceCode) {
        ControlRequestVO requestVO = ControlUtil.setControlRequestVO(type, detailType, "02", "0200010205", controlValue, referenceCode);
        pcsClient.setControlRequest(requestVO);
    }

    /**
     * 방전 모드 제어
     *
     * @param type       제어 요청 구분
     * @param detailType 제어 요청 상세 구분
     */
    private void controlDischarge(String type, String detailType, String controlValue) {
        ControlRequestVO requestVO = ControlUtil.setControlRequestVO(type, detailType, "02", "0200010206", controlValue, null);
        pcsClient.setControlRequest(requestVO);
    }

    private void controlFail() {

    }

    /**
     * 출력 전력 제어
     *
     * @param referencePower 참조 전력 값
     */
    private void controlOutputPowerOld(int referencePower) {
        int limitPower = essManager.calculateLimitPower();

        if (limitPower < referencePower) {
            String controlValue = String.valueOf(limitPower);

            ControlRequestVO requestVO = ControlUtil.setControlRequestVO("04", "0402", "02", "0200010205", controlValue, null);
            pcsClient.setControlRequest(requestVO);
        }
    }

    private void controlOutputPower(String type, String detailType, String operationMode, int referencePower) {
        String controlCode = null;
        String controlValue = null;

        int limitPower = essManager.calculateLimitPower();

        switch (detailType) {
            case "0402":
                if (limitPower < referencePower) {
                    if (operationMode.equals("1")) {
                        controlCode = "0200010205";
                    } else if (operationMode.equals("2")) {
                        controlCode = "0200010206";
                    }

                    controlValue = String.valueOf(limitPower);
                }
                break;
            case "0503":
                if (limitPower > referencePower) {
                    controlCode = "0200010205";
                    controlValue = String.valueOf(referencePower);
                }
                break;
            case "0504":
                if (limitPower > referencePower) {
                    controlCode = "0200010206";
                    controlValue = String.valueOf(referencePower);
                }
                break;
        }

        System.out.println(controlValue);

        if (controlCode != null) {
            ControlRequestVO requestVO = ControlUtil.setControlRequestVO(type, detailType, "02", controlCode, controlValue, null);
            pcsClient.setControlRequest(requestVO);
        }
    }

    /**
     * PCS 제어
     *
     * @param pcsVO PCS 정보
     */
    public void controlPCS(PcsVO pcsVO) {
        boolean isRackOperate = essManager.isRackOperation();

        //System.out.println("평균 SoC : " + averageSoC);
        //System.out.println("Rack 운영 가능 여부 : " + isRackOperate);

        if (isRackOperate) {
            String operationMode = pcsVO.getOperationModeStatus();
            float averageSoC = essManager.averageSoC();

            //제어 요청이 없는 경우에 실행
            if (!pcsClient.isControlRequest()) {
                /*String evChargerRequest = evChargerClientNew.getControlRequest();

                //System.out.println("평균 SoC : " + averageSoC);

                //EV 충전기 요청 확인
                if (evChargerRequest != null) {
                    System.out.println("EV 충전기 요청 존재 : " + evChargerRequest);
                    controlByEVCharger(evChargerRequest, operation, operationMode, averageSoC);
                }*/

                if (autoControlFlag.equals("Y")) {
                    System.out.println("[ESS 제어] 자동 제어 모드 / EV 충전기에 의한 제어 가능");
                    boolean isUsingEVCharger = essManager.isUsingEVCharger();

                    if (isUsingEVCharger) {
                        isChargingEVCharger = true;
                    } /*else {
                        isChargingEVCharger = true;
                    }*/

                    if (isChargingEVCharger) {
                        System.out.println("[EV 충전기] 충전 중");
                        controlByEVChargerNew(pcsVO, averageSoC);
                    }
                } else {
                    System.out.println("[ESS 제어] 수동 제어 모드 / EV 충전기에 의한 제어 불가");
                }



                /*if (isUsingEVCharger) {
                    controlByEVChargerNew(pcsVO, averageSoC);
                } else {
                    switch (operationMode) {
                        case "0":
                            break;
                        case "1":
                            break;
                        case "2":
                            break;
                    }
                }*/

                controlBySoC(pcsVO, averageSoC);
            }
        }
    }

    /**
     * SoC에 의한 제어
     * <p>
     * - 현재 SoC에 따라 PCS 제어 기능
     *
     * @param pcsVO      PCS 정보
     * @param averageSoC 평균 SoC
     */
    private void controlBySoC(PcsVO pcsVO, float averageSoC) {
        String operation = pcsVO.getOperationStatus();
        String operationMode = pcsVO.getOperationModeStatus();
        String configCode = PmsVO.pcsConfigs.get("03").getConfigCode();  //SoC 환경설정 코드

        switch (operationMode) {
            case "0":   //대기
                //최소 유지 SoC 확인
                if (averageSoC < MIN_HOLDING_SOC) {
                    controlByMinSoC(operation, configCode, pcsVO);
                }
                break;
            case "1":   //충전
                //최소 SoC 유지 운전 상태인지 확인
                if (!isHoldingMinSoC) {
                    if (averageSoC >= MAX_OPERATION_SOC) {
                        controlStop("03", "0302", configCode);   //정지 - 최대 SoC
                    } else {
                        //controlOutputPower(pcsVO.getReferencePower());  //충전 출력 전력 조절
                        controlOutputPower("04", "0401", operationMode, pcsVO.getReferencePower());
                    }
                } else {
                    if (averageSoC >= MIN_HOLDING_SOC) {
                        controlStop("03", "0301", configCode);   //정지 - 최소 SoC
                        isHoldingMinSoC = false;
                    } else {
                        //controlOutputPower(pcsVO.getReferencePower());  //충전 출력 전력 조절
                        controlOutputPower("04", "0401", operationMode, pcsVO.getReferencePower());
                    }
                }
                break;
            case "2":   //방전
                if (averageSoC <= MIN_OPERATION_SOC) {
                    controlStop("03", "0301", configCode);    //정지 - 최소 SoC
                }
                break;
        }
    }

    /**
     * 최소 SoC에 의한 제어
     *
     * @param operation  운전 상태
     * @param configCode 환경설정 코드
     * @param pcsVO      PCS 정보
     */
    private void controlByMinSoC(String operation, String configCode, PcsVO pcsVO) {
        //PCS 운전 상태 확인 (11: 정지, 12: 운전)
        if (operation.equals("11")) {
            if (pcsVO.getWarningFlag().equals("N") && pcsVO.getFaultFlag().equals("N")) {
                controlRun("03", "0300", configCode); //운전
            }
        } else if (operation.equals("12")) {
            //최소 SoC 유지 운전 상태인지 확인
            if (!isHoldingMinSoC) {

                //수정 필요
                /*evChargerClientNew.request();   //EV 충전기 API 요청
                List<EVChargerVO> standbyChargers = evChargerClientNew.getEVChargers("ess-charge");    //대기 상태의 충전기 목록 - ESS 충전 가능 여부 확인을 위해 대기 상태인 EV 충전기 목록 호출
                int totalChargerCount = evChargerClientNew.getTotalChargerCount();  //총 EV 충전기 개수

                if (standbyChargers.size() == totalChargerCount) {
                    int limitPower = essManager.calculateLimitPower();
                    String controlValue = String.valueOf(limitPower);

                    controlCharge("03", "0301", controlValue, configCode);    //충전 - 최소 SoC
                    isHoldingMinSoC = true;
                } else if (standbyChargers.size() < totalChargerCount) {
                    controlCharge("03", "0301", "5", configCode);    //충전 - 최소 SoC
                    isHoldingMinSoC = true;

                    System.out.println("EV 충전기 일부 충전 중 ESS 저전력 충전 가능!");
                }*/
            }
        }
    }

    /**
     * EV 충전기에 의한 제어 실행
     *
     * @param pcsVO
     * @param averageSoC
     */
    private void controlByEVChargerNew(PcsVO pcsVO, float averageSoC) {
        String operation = pcsVO.getOperationStatus();
        String operationMode = pcsVO.getOperationModeStatus();
        float pcsPower = pcsVO.getOutputPower();
        float pcsReferencePower = pcsVO.getReferencePower();

        float totalEVChargerPower = essManager.getTotalEVChargerPower();
        int dischargePower = (int) (Math.ceil(totalEVChargerPower) * -1);   //방전 전력

        System.out.println("[EV 충전기] 필요 방전 전력 = " + dischargePower);

        if (!pcsClient.isControlRequest()) {
            //PCS 운전 상태 확인 (11: 정지, 12: 운전)
            if (operation.equals("11")) {
                //현재 SoC가 최소 운영 SoC에 충족하면 PCS 운전 진행
                if (averageSoC > MIN_OPERATION_SOC) {
                    System.out.println("[EV 충전기] PCS 운전 제어");
                    controlRun("05", "0500", null); //EV 충전기 충전을 위한 PCS 운전
                } /*else {
                controlStop("05", "0506", null);    //최소 운영 SoC를 충족하지 못하여 PCS 운전 취소
                //취소 로직 추 - SoC 체크 상단으로 이동 필요
            }*/
            } else if (operation.equals("12")) {
                //PCS 운전 모드 상태에 따라 처리
                switch (operationMode) {
                    case "0":   //대기
                        //현재 SoC가 최소 운영 SoC에 충족하면 PCS 방전 진행
                        if (averageSoC > MIN_OPERATION_SOC) {
                            System.out.println("참조 전력 : " + pcsReferencePower);
                            /*
                             * 방전 모드로 전환되는 시간 간격이 있어 제어가 계속 발생하는 현상이 발생하여
                             * PCS 참조 전력이 변경되어 있으면 제어가 계속 발생되지 않도록 예외처리
                             */
                            if (pcsReferencePower == 0) {
                                //현재 PMS 내부 전력 영점 조절이 '-2.0kW' 기준이여서 '-5kW'로 임시 변경
                                /*if (dischargePower < -2.0) {
                                    dischargePower = -5;
                                }*/
                                dischargePower = -20;

                                controlDischarge("05", "0502", String.valueOf(dischargePower)); //ESS 방전
                                System.out.println("[EV 충전기] 방전 가능 / PCS 방전 제어 = " + dischargePower);
                            }

                            //if (pcsReferencePower <= 0 && pcsReferencePower == dischargePower) {
                            //테스트 용
                            /*if (pcsReferencePower <= 0) {
                                //controlDischarge("05", "0502", String.valueOf(dischargePower)); //ESS 방전
                                controlDischarge("05", "0502", "-5"); //ESS 방전
                                System.out.println("[EV 충전기] 방전 가능 / PCS 방전 제어 = " + dischargePower);
                            }*/
                        }
                        break;
                    case "1":   //충전
                        float totalUsingPower = pcsPower + totalEVChargerPower;   //사용 중인 총 전력
                        float usableChargePower = essManager.CONTRACT_POWER - totalEVChargerPower;  //사용 가능한 전력

                        System.out.println("[EV 충전기] PCS 전력 = " + pcsPower + " / 총 EV 충전 전력 = " + totalEVChargerPower);
                        System.out.println("[EV 충전기] 계약 전력 = " + essManager.CONTRACT_POWER + " / 총 전력 합 = " + totalUsingPower);

                        //계약 전력과 총 사용중인 전력 비교(초과: 운전 대기 변경 및 ESS 충전 전력 감축 / 미만: ESS 충전 전력 증강)
                        if (totalUsingPower > essManager.CONTRACT_POWER) {
                            System.out.println("[EV 충전기] 계약 전력 미만 / 충전 가능 전력 = " + usableChargePower);

                            //ESS 충전에 사용 가능한 전력 확인(5kW 미만: 대기 상태 변경 / 5kW 이상: 충전 전력 변경)
                            if (usableChargePower < 5) {
                                controlAutoStandby("05", "0501");   //운전 모드 대기로 변경
                                System.out.println("[EV 충전기] 충전 가능 전력 5kW 미만 / 대기 모드 변경");
                            } else if (usableChargePower >= 5) {
                                int chargePower = (int) Math.floor(usableChargePower);

                                if (chargePower > pcsReferencePower) {
                                    controlOutputPower("05", "0503", operationMode, chargePower);   //ESS 충전 전력 변경
                                    System.out.println("[EV 충전기] 충전 가능 전력 5kW 이상 / 충전 전력 변경 = " + chargePower);
                                }
                            }
                        } else if (totalUsingPower < essManager.CONTRACT_POWER) {
                            System.out.println("[EV 충전기] 계약 전력 초과 / 충전 가능 전력 = " + usableChargePower);

                            int chargePower = (int) Math.floor(usableChargePower);

                            //방전 전력에 따라 충전 전력 조절, 방전 전력이 0이 되면 EV 충전기 종료 상태
                            if (dischargePower < 0) {
                                if (chargePower > pcsReferencePower) {
                                    controlOutputPower("05", "0503", operationMode, chargePower);   //ESS 충전 전력 변경
                                    System.out.println("[EV 충전기] EV 충전 중 / 충전 전력 변경 = " + chargePower);
                                }
                            } else if (dischargePower == 0) {
                                controlOutputPower("05", "0505", operationMode, chargePower);   //EV 충전 종료로 인한 ESS 충전 전력 변경
                                isChargingEVCharger = essManager.isUsingEVCharger();

                                System.out.println("[EV 충전기] EV 충전 종료 / 충전 전력 변경 = " + chargePower);
                            }
                        }
                        break;
                    case "2":   //방전
                        if (dischargePower < 0) {
                            if (dischargePower < -2.0) {
                                dischargePower = -5;
                            }

                            if (dischargePower > pcsReferencePower) {
                                //controlOutputPower("05", "0504", operationMode, dischargePower);    //ESS 방전 전력 조절
                                controlOutputPower("05", "0504", operationMode, dischargePower);    //ESS 방전 전력 조절
                                System.out.println("[EV 충전기] 방전 전력 확인 / 방전 전력 조절 = " + dischargePower);
                            }
                        } else if (dischargePower == 0) {
                            controlStop("05", "0505", null);    //EV 충전 종료
                            isChargingEVCharger = essManager.isUsingEVCharger();

                            System.out.println("[EV 충전기] EV 충전 종료 / PCS 방전 종료 = " + dischargePower);
                        }
                        break;
                }
            }
        }
    }

    /*private void controlByEVCharger(String request, String operation, String operationMode, float averageSoC) {
        //PCS 운전 상태 확인 (11: 정지, 12: 운전)
        if (operation.equals("11")) {
            //if (request.equals("charging") || request.equals("all-charging")) {
            //EV 충전기 충전 준비
            if (request.equals("ready")) {
                //SoC가 최소 운영 SoC보다 큰 경우 실행
                if (averageSoC > MIN_OPERATION_SOC) {
                    controlRun("05", "0500", null); //PCS 운전
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
                                controlDischarge("05", "0502", ""); //ESS 방전 - 수정 필요
                                System.out.println("[EV 충전기] 제어 요청 - ESS 방전");
                            } else {
                                evChargerClientNew.resetControlRequest();
                            }
                            break;
                        case "1":   //ESS 충전 상태
                            if (request.equals("charging")) {
                                controlCharge("05", "0503", "5", null); //ESS 저전력 충전
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

                            controlCharge("05", "0503", controlValue, null);    //전력 변경
                        }

                        evChargerClientNew.resetControlRequest();
                        System.out.println("[EV 충전기] 제어 요청 - 충전 전력 변경 : 모든 EV 충전기 종료");
                    } else if (operationMode.equals("2")) {
                        controlStop("05", "0504", null);    //EV 충전 종료 - PCS 운전 종료
                        System.out.println("[EV 충전기] 제어 요청 - PCS 운전 종료: 모든 EV 충전기 종료");
                    }
                    break;
                case "cancel":
                    if (operationMode.equals("0")) {
                        controlStop("05", "0591", null);    //EV 충전 취소 - PCS 운전 종료
                        System.out.println("[EV 충전기] 제어 요청 - PCS 운전 종료: EV 충전 취소");
                    }
                    break;
                case "error":
                    if (operationMode.equals("2")) {
                        controlStop("05", "0599", null);    //EV 충전 오류 - PCS 운전 종료
                        System.out.println("[EV 충전기] 제어 요청 - PCS 운전 종료: EV 충전 오류");
                    }
                    break;
            }
        }
    }*/
}
