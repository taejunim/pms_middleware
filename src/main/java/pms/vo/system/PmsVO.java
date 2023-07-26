package pms.vo.system;

import java.util.*;

public class PmsVO {
    /* ESS 정보 */
    public static EssVO ess;
    public static Map<String, List<EssVO.ConfigVO>> operationConfig = new HashMap<>();
    public static Map<Object, EssVO.ConfigVO> bmsConfigs = new HashMap<>();
    public static Map<Object, EssVO.ConfigVO> pcsConfigs = new HashMap<>();

    /* 코드 정보 */
    public static Map<Object, CommonCodeVO> commonCodes;
    public static Map<Object, CommonCodeVO> operationModeCodes;
    public static Map<Object, CommonCodeVO> deviceStatusCodes;
    public static Map<Object, CommonCodeVO> deviceCategoryCodes;
    public static Map<Object, CommonCodeVO> deviceCategorySubCodes;
    public static Map<Object, CommonCodeVO> requestTypeCodes;
    public static Map<Object, CommonCodeVO> requestDetailTypeCodes;

    /* 장비 정보 */
    public static List<DeviceVO> racks;
    public static Map<String, List<DeviceVO.ComponentVO>> modules = new HashMap<>();
    public static DeviceVO pcs;
    public static Map<String, DeviceVO> converters = new HashMap<>();
    public static Map<String, List<DeviceVO.ComponentVO>> inverters = new HashMap<>();
    public static Map<String, List<DeviceVO>> sensors = new HashMap<>();
    public static Map<String, List<DeviceVO>> meters = new HashMap<>();
    public static Map<String, List<DeviceVO>> airConditioners = new HashMap<>();
    public static DeviceVO middleware;

    /* 장비 제어 코드 정보 */
    public static Map<Object, DeviceVO.ControlVO> controlCodes;

    /* 장비 오류 코드 정보 */
    public static Map<Object, DeviceVO.ErrorCodeVO> commonErrorCodes;
    public static Map<Object, DeviceVO.ErrorCodeVO> bmsErrorCodes;
    public static Map<Object, DeviceVO.ErrorCodeVO> pcsErrorCodes;
}
