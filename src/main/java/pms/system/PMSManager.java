package pms.system;

import pms.common.util.DateTimeUtil;
import pms.common.util.ResourceUtil;
import pms.database.ConnectionPool;
import pms.database.query.ControlQuery;
import pms.database.query.DeviceErrorQuery;
import pms.database.query.DeviceQuery;
import pms.database.query.SystemQuery;
import pms.system.ess.ESSScheduleManager;
import pms.vo.system.*;

import java.util.*;

public class PMSManager {
    public static final Properties applicationProperties = ResourceUtil.loadProperties("application");
    private final SystemQuery systemQuery = new SystemQuery();
    private final DeviceQuery deviceQuery = new DeviceQuery();
    private final ControlQuery controlQuery = new ControlQuery();
    private final DeviceErrorQuery deviceErrorQuery = new DeviceErrorQuery();

    /**
     * 시스템 초기 설정
     */
    public void initSystem() {
        ConnectionPool.initConnectionPool();    //Database Connection Pool 초기화
        setSystemInfo();
    }

    /**
     * 시스템 정보 설정
     */
    private void setSystemInfo() {
        setESS();
        setCommonCode();
        setOperationConfig();
        setOperationSchedule();
        setDeviceInfo();
        setDeviceControlCode();
        setDeviceErrorCode();
    }

    private void setESS() {
        PmsVO.ess = systemQuery.getESS();
    }

    private void setOperationConfig() {
        for (Object key : PmsVO.deviceCategorySubCodes.keySet()) {
            CommonCodeVO categorySubVO = PmsVO.deviceCategorySubCodes.get(key);
            String categorySub = categorySubVO.getCode();  //장비 하위 분류 코드

            Map<Object, EssVO.ConfigVO> configMap = systemQuery.getOperationConfigMap(categorySub);

            switch (categorySub) {
                case "0101":
                    PmsVO.bmsConfigs = configMap;
                    break;
                case "0200":
                    PmsVO.pcsConfigs = configMap;
                    break;
            }
        }
    }

    private void setOperationSchedule() {
        String currentDate = DateTimeUtil.getDateFormat("yyyyMMdd");
        String currentTime = DateTimeUtil.getDateFormat("HHmmss");
        List<ScheduleVO> schedules = systemQuery.getScheduleList(currentDate);

        Map<String, ScheduleVO> scheduleMap = new HashMap<>();
        Map<String, List<ScheduleVO.ScheduleDetailVO>> scheduleDetailMap = new HashMap<>();

        for (ScheduleVO scheduleVO : schedules) {
            String scheduleDate = scheduleVO.getScheduleDate();
            List<ScheduleVO.ScheduleDetailVO> scheduleDetails;

            if (Integer.parseInt(scheduleDate) > Integer.parseInt(currentDate)) {
                scheduleDetails = systemQuery.getScheduleDetailList(scheduleDate, "000000");
            } else {
                scheduleDetails = systemQuery.getScheduleDetailList(scheduleDate, currentTime);
            }

            scheduleMap.put(scheduleDate, scheduleVO);
            scheduleDetailMap.put(scheduleDate, scheduleDetails);
        }

        ESSScheduleManager.schedules = scheduleMap;
        ESSScheduleManager.schedulesMap = scheduleDetailMap;
    }

    /**
     * 공통 코드 정보 설정
     */
    private void setCommonCode() {
        PmsVO.commonCodes = systemQuery.getCommonCodeMap();
        PmsVO.operationModeCodes = systemQuery.getCommonCodeByGroup("OPERATION_MODE", null);
        PmsVO.deviceStatusCodes = systemQuery.getCommonCodeByGroup("DEVICE_STATUS", null);
        PmsVO.requestTypeCodes = systemQuery.getCommonCodeByGroup("CONTROL_REQUEST_TYPE", null);
        PmsVO.requestDetailTypeCodes = systemQuery.getCommonCodeByGroup("CONTROL_REQUEST__DETAIL_TYPE", null);

        setDeviceCategory();
        setDeviceCategorySub();
    }

    /**
     * 공통 코드 정보 설정
     * <p>
     * - 장비 분류 정보 설정
     */
    private void setDeviceCategory() {
        Map<String, List<String>> parameterMap = new HashMap<>();
        String[] data1Values = new String[]{"00", PmsVO.ess.getEssType()};  //00: 공통, 01: 고정형 ESS/02: 이동형 ESS
        parameterMap.put("DATA1", new ArrayList<>(Arrays.asList(data1Values))); //공통 코드의 DATA1 컬렴 참조

        PmsVO.deviceCategoryCodes = systemQuery.getCommonCodeByGroup("DEVICE_CATEGORY", parameterMap);
    }

    /**
     * 공통 코드 정보 설정
     * <p>
     * - 장비 하위 분류 정보 설정
     */
    private void setDeviceCategorySub() {
        Map<String, List<String>> parameterMap = new HashMap<>();
        String[] data1Values = new String[]{"00", PmsVO.ess.getEssType()};  //00: 공통, 01: 고정형 ESS/02: 이동형 ESS
        parameterMap.put("DATA1", new ArrayList<>(Arrays.asList(data1Values))); //공통 코드의 DATA1 컬렴 참조

        PmsVO.deviceCategorySubCodes = systemQuery.getCommonCodeByGroup("DEVICE_CATEGORY_SUB", parameterMap);
    }

    /**
     * 장비 정보 설정
     */
    private void setDeviceInfo() {
        //장비 분류에 따라 각 장비 정보 설정
        for (Object key : PmsVO.deviceCategorySubCodes.keySet()) {
            CommonCodeVO categorySubVO = PmsVO.deviceCategorySubCodes.get(key);
            String category = categorySubVO.getData2();    //장비 분류 코드
            String categorySub = categorySubVO.getCode();  //장비 하위 분류 코드

            switch (category) {
                case "01":  //BMS
                    if (categorySub.equals("0101")) {
                        setBMS(category, categorySub);
                    }
                    break;
                case "02":  //PCS
                    setPCS(category, categorySub);
                    break;
                case "03":  //이동형 PCS
                    break;
                case "04":  //센서
                    setSensors(category, categorySub);
                    break;
                case "80":  //공조장치
                    setAirConditioners(category, categorySub);
                    break;
                case "90":  //프로그램
                    //Middleware
                    if (categorySub.equals("9001")) {
                        setMiddleware(category, categorySub);
                    }
            }
        }
    }

    private void setDeviceControlCode() {
        PmsVO.controlCodes = controlQuery.getControlCodes("CONTROL_CODE");
    }

    /**
     * 장비 오류 코드 정보 설정
     */
    private void setDeviceErrorCode() {
        PmsVO.commonErrorCodes = deviceErrorQuery.getDeviceErrorCodeMap("00", "REFERENCE_CODE"); //공통 오류 코드 설정

        //장비 분류에 따라 각 장비 오류 코드 정보 설정
        for (Object key : PmsVO.deviceCategoryCodes.keySet()) {
            CommonCodeVO categoryVO = PmsVO.deviceCategoryCodes.get(key);
            String category = categoryVO.getCode();    //장비 분류 코드

            switch (category) {
                case "01":
                    PmsVO.bmsErrorCodes = deviceErrorQuery.getDeviceErrorCodeMap(category, "REFERENCE_CODE");
                    break;
                case "02":
                    PmsVO.pcsErrorCodes = deviceErrorQuery.getDeviceErrorCodeMap(category, "REFERENCE_CODE");
                    break;
                /*case "03":
                    break;
                case "04":
                    break;
                case "80":
                    break;*/
            }
        }
    }

    /**
     * BMS 정보 설정
     *
     * @param category    장비 분류 코드
     * @param categorySub 장비 하위 분류 코드
     */
    private void setBMS(String category, String categorySub) {
        PmsVO.racks = deviceQuery.getDeviceList(category, categorySub);   //Rack 정보 설정

        //Rack 별 Module 정보 설정
        for (DeviceVO rack : PmsVO.racks) {
            String rackCode = rack.getDeviceCode(); //Rack 코드
            List<DeviceVO.ComponentVO> moduleList = deviceQuery.getComponentList(rackCode); //Module 정보

            PmsVO.modules.put(rackCode, moduleList);
        }
    }

    /**
     * PCS 정보 설정
     *
     * @param category    장비 분류 코드
     * @param categorySub 장비 하위 분류 코드
     */
    private void setPCS(String category, String categorySub) {
        PmsVO.pcs = deviceQuery.getDevice(category, categorySub);
    }

    /**
     * 센서 정보 설정
     * <p>
     * 센서 하위 분류별로 센서 정보를 설정
     *
     * @param category    장비 분류 코드
     * @param categorySub 장비 하위 분류 코드
     */
    private void setSensors(String category, String categorySub) {
        List<DeviceVO> sensorsByCategory = deviceQuery.getDeviceList(category, categorySub);  //센서 하위 분류별 센서 목록

        PmsVO.sensors.put(categorySub, sensorsByCategory);
    }

    /**
     * 공조장치 정보 설정
     *
     * @param category    장비 분류 코드
     * @param categorySub 장비 하위 분류 코드
     */
    private void setAirConditioners(String category, String categorySub) {

    }

    private void setMiddleware(String category, String categorySub) {
        PmsVO.middleware = deviceQuery.getDevice(category, categorySub);
    }
}