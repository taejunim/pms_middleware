package pms.system.ess;

import pms.common.util.DateTimeUtil;
import pms.communication.external.smarthub.EVChargerClientNew;
import pms.communication.external.switchboard.meter.PowerMeterClient;
import pms.system.PMSCode;
import pms.vo.device.control.ControlRequestVO;
import pms.vo.device.control.ControlResponseVO;
import pms.vo.device.external.EVChargerVO;
import pms.vo.history.ControlHistoryVO;
import pms.vo.system.DeviceVO;
import pms.vo.system.PmsVO;

import java.util.List;

public class ControlUtil {

    private static int getAddress(String deviceCategory, String deviceCode, String controlType) {
        int address = 0;

        switch (deviceCategory) {
            case "01":
                address = getRackControlAddress(controlType);
                break;
            case "02":
                address = getPCSControlAddress(controlType);
                break;
            case "03":
                address = getConverterControlAddress(deviceCode, controlType);
                break;
            case "80":
                address = getAirConditionerAddress(deviceCode, controlType);
                break;
            case "90":
                address = Integer.parseInt(controlType);
                break;
        }

        return address;
    }

    /**
     * 에어컨 제어 주소 호출 - 차후 수정 필요
     *
     * @param deviceCode
     * @param controlType
     * @return
     */
    private static int getAirConditionerAddress(String deviceCode, String controlType) {
        int address = 0;

        if (controlType.equals("8098") || controlType.equals("8099")) {
            if (deviceCode.equals("800201")) {
                address = 1;
            } else if (deviceCode.equals("800202")) {
                address = 2;
            }
        }

        return address;
    }

    private static int getRackControlAddress(String controlType) {
        int address = 0;

        switch (controlType) {
            case "0100":    //40002: 초기화
                address = 2;
                break;
            case "0101":    //40003: 정상운영
                address = 3;
                break;
            case "0102":    //40001: 비상정지
                address = 1;
                break;
        }

        return address;
    }

    private static int getPCSControlAddress(String controlType) {
        int address;

        switch (controlType) {
            case "0200":    //초기화
                address = 2;
                break;
            case "0201":    //운전
            case "0202":    //정지
                address = 1;
                break;
            case "0203":    //비상정지
                address = 8;
                break;
            case "0204":    //대기
            case "0205":    //충전
            case "0206":    //방전
                address = 5;
                break;
            default:
                address = 6;
                break;
        }

        return address;
    }

    private static int getConverterControlAddress(String deviceCode, String controlType) {
        if (deviceCode.equals("030101")) {
            switch (controlType) {
                case "0300":    //운전 모드 선택 - 0: 선택 안함
                case "0301":    //운전 모드 선택 - 1: 충전
                case "0302":    //운전 모드 선택 - 2: 방전
                    return 214;
                case "0303":    //초기화 시작
                case "0304":    //초기화 종료
                    return 203;
                case "0305":    //전류 설정
                    return 314;
                case "0307":    //L-Inverter 운전
                case "0308":    //L-Inverter 정지
                    return 206;
                case "0309":    //R-Inverter 운전
                case "0310":    //R-Inverter 정지
                case "0306":    //AC/DC 컨버터 비상정지
                    return 212;
            }
        } else if (deviceCode.equals("030201")) {
            //DC/DC 컨버터 비상정지
            if (controlType.equals("0311")) {
                return 206; //주소 불명 - 추후 변경 예정
            }
        }

        return 0;
    }

    private static int setPowerValue(String controlType) {
        int powerValue = 0;

        if (controlType.equals("0205")) {
            ESSManager essManager = new ESSManager();
            essManager.getTotalEVChargerPower();

            float totalEVChargerPower = essManager.getTotalEVChargerPower();
            float usableChargePower = essManager.CONTRACT_POWER - totalEVChargerPower;  //사용 가능한 전력

            if (usableChargePower == essManager.CONTRACT_POWER) {
                powerValue = new ESSManager().calculateLimitPower();
            } else {
                if (usableChargePower > 5) {
                    powerValue = (int) Math.floor(usableChargePower);
                }
            }

            /*EVChargerClientNew evChargerClientNew = new EVChargerClientNew();
            evChargerClientNew.request();   //EV 충전기 API 요청

            List<EVChargerVO> standbyChargers = evChargerClientNew.getEVChargers("ess-charge");    //대기 상태의 충전기 목록 - ESS 충전 가능 여부 확인을 위해 대기 상태인 EV 충전기 목록 호출

            int totalChargerCount = evChargerClientNew.getTotalChargerCount();  //총 EV 충전기 개수
            int standbyChargerCount = standbyChargers.size();

            if (standbyChargerCount > 0) {
                if (standbyChargerCount == totalChargerCount) {
                    powerValue = new ESSManager().calculateLimitPower();
                    System.out.println("전력 값 : " + powerValue);
                } else if (standbyChargerCount < totalChargerCount) {
                    System.out.println("EV 충전기 일부 충전 중 ESS 저전력 충전 가능!");
                    powerValue = 5;
                }
            } else {
                System.out.println("EV 충전기 모두 충전 중 ESS 충전 불가!");
            }*/
        }

        return powerValue;
    }

    public static ControlRequestVO setRemoteControlRequestVO(String remoteId, String type, String deviceCategory, String controlCode, String requestControlValue, String controllerId) {
        System.out.println("제어생성!");
        int requestDate = DateTimeUtil.getUnixTimestamp();
        DeviceVO.ControlVO controlVO = PMSCode.getControlVO(controlCode);
        String deviceCode = controlVO.getDeviceCode();
        String controlType = controlVO.getControlType();

        int address = getAddress(deviceCategory, deviceCode, controlType);
        int controlValue;

        if (requestControlValue.isEmpty()) {
            controlValue = controlVO.getControlValue();
        } else {
            controlValue = Integer.parseInt(requestControlValue);
        }

        System.out.println("control Value : " + controlValue);

        //고정형 ESS 전력 값 확인 및 조정
        if (PmsVO.ess.getEssType().equals("01")) {
            if (controlType.equals("0205")) {
                controlValue = setPowerValue(controlType);

                if (controlValue == 0) {
                    return null;
                }
            }
        }

        /*if (controlType.equals("0205")) {
            EVChargerClientNew evChargerClientNew = new EVChargerClientNew();
            evChargerClientNew.request();   //EV 충전기 API 요청

            List<EVChargerVO> standbyChargers = evChargerClientNew.getEVChargers("ess-charge");    //대기 상태의 충전기 목록 - ESS 충전 가능 여부 확인을 위해 대기 상태인 EV 충전기 목록 호출

            int totalChargerCount = evChargerClientNew.getTotalChargerCount();  //총 EV 충전기 개수
            int standbyChargerCount = standbyChargers.size();

            if (standbyChargerCount > 0) {
                if (standbyChargerCount == totalChargerCount) {
                    controlValue = new ESSManager().calculateLimitPower();
                    System.out.println("전력 값 : " + controlValue);
                } else if (standbyChargerCount < totalChargerCount) {
                    controlValue = 5;
                    System.out.println("EV 충전기 일부 충전 중 ESS 저전력 충전 가능!");
                }
            } else {
                System.out.println("EV 충전기 모두 충전 중 ESS 충전 불가!");
                return null;
            }
        }*/

        ControlRequestVO requestVO = new ControlRequestVO();
        requestVO.setRemoteId(remoteId);
        requestVO.setType(type);
        requestVO.setDate(requestDate);
        requestVO.setAddress(address);
        requestVO.setDeviceCode(controlVO.getDeviceCode());
        requestVO.setControlType(controlVO.getControlType());
        requestVO.setControlCode(controlCode);
        requestVO.setControlValue(controlValue);
        requestVO.setControllerId(controllerId);

        System.out.println("Control Value : " + controlValue);

        return requestVO;
    }

    public static ControlRequestVO setControlRequestVO(String type, String detailType, String deviceCategory, String controlCode, String controlValue, String referenceCode) {
        int requestDate = DateTimeUtil.getUnixTimestamp();
        DeviceVO.ControlVO controlVO = PMSCode.getControlVO(controlCode); //PmsVO.controlCodes.get(controlCode);
        String deviceCode = controlVO.getDeviceCode();
        String controlType = controlVO.getControlType();

        int address = getAddress(deviceCategory, deviceCode, controlType);

        ControlRequestVO requestVO = new ControlRequestVO();
        requestVO.setType(type);
        requestVO.setDetailType(detailType);
        requestVO.setDate(requestDate);
        requestVO.setAddress(address);
        requestVO.setDeviceCode(controlVO.getDeviceCode());
        requestVO.setControlType(controlType);
        requestVO.setControlCode(controlCode);
        requestVO.setReferenceCode(referenceCode);
        requestVO.setControllerId("system");

        if (controlValue == null || controlValue.equals("")) {
            requestVO.setControlValue(controlVO.getControlValue());
        } else {
            requestVO.setControlValue(Integer.parseInt(controlValue));
        }

        return requestVO;
    }

    public static ControlResponseVO setControlResponseVO(int responseAddress, short responseValue, ControlRequestVO requestVO) {
        int result = 0;
        int requestAddress = requestVO.getAddress();
        int requestValue = requestVO.getControlValue();

        if (requestAddress == responseAddress) {
            if (requestValue == responseValue) {
                result = 1;
            }
        }

        ControlResponseVO responseVO = new ControlResponseVO();
        responseVO.setResult(result);
        responseVO.setRequestVO(requestVO);

        ControlHistoryVO controlHistoryVO = setControlHistoryVO(responseVO);
        responseVO.setHistoryVO(controlHistoryVO);

        return responseVO;
    }

    private static ControlHistoryVO setControlHistoryVO(ControlResponseVO responseVO) {
        String completeFlag;

        if (responseVO.getResult() == 1) {
            completeFlag = "Y";
        } else {
            completeFlag = "N";
        }

        ControlRequestVO requestVO = responseVO.getRequestVO();

        ControlHistoryVO controlHistoryVO = new ControlHistoryVO();
        controlHistoryVO.setControlCode(requestVO.getControlCode());
        controlHistoryVO.setControlDate(requestVO.getDate());
        controlHistoryVO.setControlRequestType(requestVO.getType());
        controlHistoryVO.setControlRequestDetailType(requestVO.getDetailType());
        controlHistoryVO.setControlRequestValue(requestVO.getControlValue());
        controlHistoryVO.setReferenceCode(requestVO.getReferenceCode());
        controlHistoryVO.setControlCompleteFlag(completeFlag);
        controlHistoryVO.setDeviceResponseDate(DateTimeUtil.getUnixTimestamp());
        controlHistoryVO.setControlRequestId(requestVO.getControllerId());

        return controlHistoryVO;
    }
}