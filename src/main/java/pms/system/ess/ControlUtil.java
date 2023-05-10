package pms.system.ess;

import pms.common.util.DateTimeUtil;
import pms.communication.external.smarthub.EVChargerClient;
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
            case "80":
                address = getAirConditionerAddress(deviceCode, controlType);
                break;
            case "90":
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

    public static ControlRequestVO setRemoteControlRequestVO(String remoteId, String type, String deviceCategory, String controlCode) {
        int requestDate = DateTimeUtil.getUnixTimestamp();
        DeviceVO.ControlVO controlVO = PMSCode.getControlVO(controlCode); //PmsVO.controlCodes.get(controlCode);
        String deviceCode = controlVO.getDeviceCode();
        String controlType = controlVO.getControlType();

        int address = getAddress(deviceCategory, deviceCode, controlType);
        int controlValue = controlVO.getControlValue();

        ControlRequestVO requestVO = new ControlRequestVO();

        if (controlType.equals("0205")) {
            /*EVChargerClient evChargerClient = new EVChargerClient();
            evChargerClient.request();

            List<EVChargerVO> chargers = evChargerClient.getEVChargers("ess-charge");
            int totalChargerCount = evChargerClient.getChargerCount();

            if (chargers.size() == totalChargerCount) {
                controlValue = new ESSManager().calculateLimitPower();
            } else if (chargers.size() < totalChargerCount) {
                controlValue = 5;
                System.out.println("EV 충전기 일부 충전 중 ESS 저전력 충전 가능!");
            } else if (chargers.size() == 0) {
                System.out.println("EV 충전기 모두 충전 중 ESS 충전 불가!");
                return null;
            }*/
        }

        requestVO.setRemoteId(remoteId);
        requestVO.setType(type);
        requestVO.setDate(requestDate);
        requestVO.setAddress(address);
        requestVO.setDeviceCode(controlVO.getDeviceCode());
        requestVO.setControlType(controlVO.getControlType());
        requestVO.setControlCode(controlCode);
        requestVO.setControlValue(controlValue);

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

        if (controlValue == null) {
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

        if (!requestVO.getType().equals("02")) {
            ControlHistoryVO controlHistoryVO = setControlHistoryVO(responseVO);
            responseVO.setHistoryVO(controlHistoryVO);
        }

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
        controlHistoryVO.setControlRequestId("system");

        return controlHistoryVO;
    }
}
