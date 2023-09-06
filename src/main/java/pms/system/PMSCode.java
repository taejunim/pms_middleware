package pms.system;

import pms.vo.system.DeviceVO;
import pms.vo.system.PmsVO;

public class PMSCode {
    public static String getCommonCode(String key) {
        if (PmsVO.commonCodes.containsKey(key))
            return PmsVO.commonCodes.get(key).getCode();
        else
            return null;
    }

    public static String getOperationMode(String key) {
        if (PmsVO.operationModeCodes.containsKey(key))
            return PmsVO.operationModeCodes.get(key).getCode();
        else
            return null;
    }

    public static String getDeviceStatus(String key) {
        if (PmsVO.deviceStatusCodes.containsKey(key))
            return PmsVO.deviceStatusCodes.get(key).getCode();
        else
            return null;
    }

    public static String getControlDevice(String key) {
        if (PmsVO.controlCodes.containsKey(key))
            return PmsVO.controlCodes.get(key).getDeviceCode();
        else
            return null;
    }

    public static DeviceVO.ControlVO getControlVO(String key) {
        return PmsVO.controlCodes.get(key);
    }

    public static String getControlCode(String key) {
        if (PmsVO.controlCodes.containsKey(key))
            return PmsVO.controlCodes.get(key).getControlCode();
        else
            return null;
    }

    public static Object getControlValue(String key) {
        if (PmsVO.controlCodes.containsKey(key))
            return PmsVO.controlCodes.get(key).getControlValue();
        else
            return null;
    }

    public static String getCommonErrorCode(String key) {
        if (PmsVO.commonErrorCodes.containsKey(key))
            return PmsVO.commonErrorCodes.get(key).getErrorCode();
        else
            return null;
    }

    public static String getCommonErrorName(String key) {
        if (PmsVO.commonErrorCodes.containsKey(key))
            return PmsVO.commonErrorCodes.get(key).getErrorCodeName();
        else
            return null;
    }

    public static String getBMSErrorCode(String key) {
        if (PmsVO.bmsErrorCodes.containsKey(key))
            return PmsVO.bmsErrorCodes.get(key).getErrorCode();
        else
            return null;
    }

    public static String getBMSErrorName(String key) {
        if (PmsVO.bmsErrorCodes.containsKey(key))
            return PmsVO.bmsErrorCodes.get(key).getErrorCodeName();
        else
            return null;
    }

    public static String getPCSErrorCode(String key) {
        if (PmsVO.pcsErrorCodes.containsKey(key))
            return PmsVO.pcsErrorCodes.get(key).getErrorCode();
        else
            return null;
    }

    public static String getPCSErrorName(String key) {
        if (PmsVO.pcsErrorCodes.containsKey(key))
            return PmsVO.pcsErrorCodes.get(key).getErrorCodeName();
        else
            return null;
    }

    public static String getAirConditionerCode(String key) {
        if (PmsVO.airConditionerErrorCodes.containsKey(key)) {
            return PmsVO.airConditionerErrorCodes.get(key).getErrorCode();
        } else {
            return null;
        }
    }

    public static String getAirConditionerName(String key) {
        if (PmsVO.airConditionerErrorCodes.containsKey(key)) {
            return PmsVO.airConditionerErrorCodes.get(key).getErrorCodeName();
        } else {
            return null;
        }
    }
}
