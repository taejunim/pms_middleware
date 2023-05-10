package pms.database.query;

import pms.database.QueryUtil;
import pms.database.SqlSession;
import pms.vo.device.AirConditionerVO;
import pms.vo.device.BmsVO;
import pms.vo.device.PcsVO;
import pms.vo.device.SensorVO;
import pms.vo.system.DeviceVO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Device Query
 * <p>
 * - 장비 관련 쿼리 호출
 */
public class DeviceQuery {
    private final SqlSession sqlSession = new SqlSession();

    /**
     * 장비 정보 조회
     *
     * @param category    장비 분류 코드
     * @param categorySub 장비 하위 분류 코드
     * @return 장비 정보
     */
    public DeviceVO getDevice(String category, String categorySub) {
        String sql = "SELECT DEVICE_CODE, " +
                "DEVICE_NO, " +
                "DEVICE_NAME, " +
                "DEVICE_CATEGORY, " +
                "DEVICE_CATEGORY_SUB, " +
                "DEVICE_ROOM " +
                "FROM BASE_DEVICE " +
                "WHERE DEVICE_CATEGORY = '" + category + "' " +
                "AND DEVICE_CATEGORY_SUB = '" + categorySub + "' " +
                "LIMIT 1";

        return sqlSession.selectOne(sql, DeviceVO.class);
    }

    /**
     * 장비 정보 목록 조회
     *
     * @param category    장비 분류 코드
     * @param categorySub 장비 하위 분류 코드
     * @return 장비 정보 List
     */
    public List<DeviceVO> getDeviceList(String category, String categorySub) {
        String sql = "SELECT DEVICE_CODE, " +
                "DEVICE_NO, " +
                "DEVICE_NAME, " +
                "DEVICE_CATEGORY, " +
                "DEVICE_CATEGORY_SUB, " +
                "DEVICE_ROOM " +
                "FROM BASE_DEVICE " +
                "WHERE DEVICE_CATEGORY = '" + category + "'";

        if (categorySub != null) {
            sql = sql + " AND DEVICE_CATEGORY_SUB = '" + categorySub + "'";
        }

        return sqlSession.selectList(sql, DeviceVO.class);
    }

    /**
     * 장비별 구성요소 정보 목록 조회
     *
     * @param deviceCode 장비 코드
     * @return 장비 구성요소 정보 List
     */
    public List<DeviceVO.ComponentVO> getComponentList(String deviceCode) {
        String sql = "SELECT DEVICE_CODE, " +
                "COMPONENT_NO, " +
                "COMPONENT_NAME " +
                "FROM BASE_DEVICE_COMPONENT " +
                "WHERE DEVICE_CODE = '" + deviceCode + "'";

        return sqlSession.selectList(sql, DeviceVO.ComponentVO.class);
    }

    public Map<Object, DeviceVO> getDeviceMap(String categorySub, String mapKey) {
        String sql = "SELECT DEVICE_CODE, " +
                "DEVICE_NO, " +
                "DEVICE_NAME, " +
                "DEVICE_CATEGORY, " +
                "DEVICE_CATEGORY_SUB, " +
                "DEVICE_ROOM " +
                "FROM BASE_DEVICE " +
                "WHERE DEVICE_CATEGORY_SUB = '" + categorySub + "'";

        return sqlSession.selectMap(sql, DeviceVO.class, mapKey);
    }

    public Map<Object, DeviceVO> getDeviceMap(String categorySub, String[] mapKeys) {
        String sql = "SELECT DEVICE_CODE, " +
                "DEVICE_NO, " +
                "DEVICE_NAME, " +
                "DEVICE_CATEGORY, " +
                "DEVICE_CATEGORY_SUB, " +
                "DEVICE_ROOM " +
                "FROM BASE_DEVICE " +
                "WHERE DEVICE_CATEGORY_SUB = '" + categorySub + "'";

        return sqlSession.selectMap(sql, DeviceVO.class, mapKeys, "_");
    }

    public int insertRackData(BmsVO.RackVO rackVO) {
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO RAW_BATTERY_RACK " +
                "(RACK_CODE, REG_DATE, OPERATION_STATUS, OPERATION_MODE_STATUS, USER_SOC, REAL_SOC, " +
                "SOH, VOLTAGE, CURRENT_SENSOR1, CURRENT_SENSOR2, " +
                "CHARGE_CURRENT_LIMIT, CHARGE_POWER_LIMIT, DISCHARGE_CURRENT_LIMIT, DISCHARGE_POWER_LIMIT, " +
                "POSITIVE_VOLTAGE_RESISTANCE, NEGATIVE_VOLTAGE_RESISTANCE, " +
                "POSITIVE_MAIN_RELAY_ACTION, POSITIVE_MAIN_RELAY_CONTACT, NEGATIVE_MAIN_RELAY_ACTION, NEGATIVE_MAIN_RELAY_CONTACT, " +
                "EMERGENCY_RELAY_ACTION, EMERGENCY_RELAY_CONTACT, PRECHARGE_RELAY_ACTION, " +
                "WARNING_FLAG, FAULT_FLAG) " +
                "VALUES "
        );

        String values = QueryUtil.createInsertValue(rackVO);
        sql.append(values);

        return sqlSession.insert(sql.toString());
    }

    public int insertModuleData(List<BmsVO.ModuleVO> modules) {
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO RAW_BATTERY_MODULE " +
                "(RACK_CODE, MODULE_NO, REG_DATE, MODULE_VOLTAGE, " +
                "CELL1_VOLTAGE, CELL2_VOLTAGE, CELL3_VOLTAGE, CELL4_VOLTAGE, " +
                "CELL5_VOLTAGE, CELL6_VOLTAGE, CELL7_VOLTAGE, CELL8_VOLTAGE, " +
                "CELL9_VOLTAGE, CELL10_VOLTAGE, CELL11_VOLTAGE, CELL12_VOLTAGE, " +
                "CELL13_VOLTAGE, CELL14_VOLTAGE, CELL15_VOLTAGE, CELL16_VOLTAGE, " +
                "CELL_BALANCING_FLAG, " +
                "CELL1_BALANCING_FLAG, CELL2_BALANCING_FLAG, CELL3_BALANCING_FLAG, CELL4_BALANCING_FLAG, " +
                "CELL5_BALANCING_FLAG, CELL6_BALANCING_FLAG, CELL7_BALANCING_FLAG, CELL8_BALANCING_FLAG, " +
                "CELL9_BALANCING_FLAG, CELL10_BALANCING_FLAG, CELL11_BALANCING_FLAG, CELL12_BALANCING_FLAG, " +
                "CELL13_BALANCING_FLAG, CELL14_BALANCING_FLAG, CELL15_BALANCING_FLAG, CELL16_BALANCING_FLAG, " +
                "CELL_TEMPERATURE1, CELL_TEMPERATURE2, CELL_TEMPERATURE3, CELL_TEMPERATURE4, " +
                "CELL_TEMPERATURE5, CELL_TEMPERATURE6, CELL_TEMPERATURE7, CELL_TEMPERATURE8) " +
                "VALUES "
        );

        List<Object> voList = new ArrayList<>(modules);

        String values = QueryUtil.createInsertValues(voList);
        sql.append(values);

        return sqlSession.insert(sql.toString());
    }

    public int insertPCSData(PcsVO pcsVO) {
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO RAW_PCS " +
                "(PCS_CODE, REG_DATE, OPERATION_STATUS, OPERATION_MODE_STATUS, " +
                "OUTPUT_POWER, RS_LINE_VOLTAGE, ST_LINE_VOLTAGE, TR_LINE_VOLTAGE, R_PHASE_CURRENT, S_PHASE_CURRENT, T_PHASE_CURRENT, " +
                "FREQUENCY, DC_LINK_VOLTAGE, BATTERY_VOLTAGE, BATTERY_CURRENT, IGBT_TEMPERATURE1, IGBT_TEMPERATURE2, IGBT_TEMPERATURE3, " +
                "AC_MAIN_MC_STATUS, DC_MAIN_MC_STATUS, ACCUMULATED_CHARGE_ENERGY, ACCUMULATED_DISCHARGE_ENERGY, REFERENCE_POWER, " +
                "EMERGENCY_STOP_FLAG, WARNING_FLAG, FAULT_FLAG) " +
                "VALUES "
        );

        String values = QueryUtil.createInsertValue(pcsVO);
        sql.append(values);

        return sqlSession.insert(sql.toString());
    }

    public int insertSensorsData(List<SensorVO> sensorsData) {
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO RAW_SENSOR" +
                " (SENSOR_CODE, REG_DATE, SENSOR_STATUS," +
                " MEASURE1, MEASURE2, MEASURE3, WARNING_FLAG, FAULT_FLAG) " +
                "VALUES "
        );

        List<Object> voList = new ArrayList<>(sensorsData);

        String values = QueryUtil.createInsertValues(voList);
        sql.append(values);

        return sqlSession.insert(sql.toString());
    }

    public int insertAirConditionersData(List<AirConditionerVO> airConditionerData) {
        StringBuilder sql = new StringBuilder();

        sql.append("INSERT INTO RAW_AIR_CONDITIONER " +
                "(AIR_CONDITIONER_CODE, REG_DATE, OPERATION_STATUS, OPERATION_MODE_STATUS, " +
                "INDOOR_TEMPERATURE, SET_TEMPERATURE, WARNING_FLAG, FAULT_FLAG) " +
                "VALUES "
        );

        List<Object> voList = new ArrayList<>(airConditionerData);

        String values = QueryUtil.createInsertValues(voList);
        sql.append(values);

        return sqlSession.insert(sql.toString());
    }
}
