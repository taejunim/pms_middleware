package pms.database.query;

import pms.common.util.DateTimeUtil;
import pms.database.QueryUtil;
import pms.database.SqlSession;
import pms.vo.system.CommonCodeVO;
import pms.vo.system.EssVO;
import pms.vo.system.PmsVO;
import pms.vo.system.ScheduleVO;

import java.util.List;
import java.util.Map;

/**
 * System Query
 * <p>
 * - 시스템 및 기본 정보 관련 쿼리 호출
 */
public class SystemQuery {
    private final SqlSession sqlSession = new SqlSession();

    /**
     * 공통 코드 정보 조회
     *
     * @return 공통 코드 정보 Map
     */
    public Map<Object, CommonCodeVO> getCommonCodeMap() {
        String sql = "SELECT GROUP_CD, " +
                "CODE, " +
                "GROUP_NM, " +
                "NAME, " +
                "SORT, " +
                "DATA1, " +
                "DATA2 " +
                "FROM SYSTEM_COMMON_CODE " +
                "WHERE USE_YN = 'Y'";

        return sqlSession.selectMap(sql, CommonCodeVO.class, new String[]{"GROUP_CD", "CODE"}, "_");
    }

    /**
     * 그룹 코드로 공통 코드 정보 조회
     *
     * @param groupCode    그룹 코드
     * @param parameterMap 조회 조건 Map
     * @return 조회 조건 공통 코드 정보 Map
     */
    public Map<Object, CommonCodeVO> getCommonCodeByGroup(String groupCode, Map<String, List<String>> parameterMap) {
        String sql = "SELECT GROUP_CD, " +
                "CODE, " +
                "GROUP_NM, " +
                "NAME, " +
                "SORT, " +
                "DATA1, " +
                "DATA2 " +
                "FROM SYSTEM_COMMON_CODE " +
                "WHERE USE_YN = 'Y' " +
                "AND GROUP_CD = '" + groupCode + "'";

        if (parameterMap != null) {
            sql = sql + QueryUtil.addConditionSql(parameterMap);
        }

        return sqlSession.selectMap(sql, CommonCodeVO.class, "CODE");
    }

    /**
     * ESS 정보 조회
     *
     * @return ESS 정보
     */
    public EssVO getESS() {
        String sql = "SELECT ESS_CODE, " +
                "ESS_TYPE, " +
                "TOTAL_CHARGE, " +
                "TOTAL_DISCHARGE, " +
                "AUTO_CONTROL_FLAG " +
                "FROM BASE_ESS " +
                "LIMIT 1";

        return sqlSession.selectOne(sql, EssVO.class);
    }

    public int updateESS(String totalCharge, String totalDischarge) {
        String sql = "UPDATE BASE_ESS " +
                "SET " +
                "TOTAL_CHARGE = '" + totalCharge + "', " +
                "TOTAL_DISCHARGE = '" + totalDischarge + "', " +
                "ENERGY_UPDATED_DATE = '" + DateTimeUtil.getCurrentTimestamp() + "' " +
                "WHERE ESS_CODE = '" + PmsVO.ess.getEssCode() + "'";

        return sqlSession.update(sql);
    }

    public List<EssVO.ConfigVO> getOperationConfig(String deviceCategorySub) {
        String sql = "SELECT DEVICE.DEVICE_CATEGORY_SUB, " +
                "CONFIG.DEVICE_CODE, " +
                "CONFIG.CONFIG_CODE, " +
                "CONFIG.CONFIG_TYPE, " +
                "CONFIG.MIN_SET_VALUE, " +
                "CONFIG.MAX_SET_VALUE " +
                "FROM OPERATION_CONFIG AS CONFIG, " +
                "BASE_DEVICE AS DEVICE " +
                "WHERE CONFIG.DEVICE_CODE = DEVICE.DEVICE_CODE " +
                "AND DEVICE.DEVICE_CATEGORY_SUB = '" + deviceCategorySub + "'";

        return sqlSession.selectList(sql, EssVO.ConfigVO.class);
    }

    public Map<Object, EssVO.ConfigVO> getOperationConfigMap(String deviceCategorySub) {
        String sql = "SELECT DEVICE.DEVICE_CATEGORY_SUB, " +
                "CONFIG.DEVICE_CODE, " +
                "CONFIG.CONFIG_CODE, " +
                "CONFIG.CONFIG_TYPE, " +
                "CONFIG.MIN_SET_VALUE, " +
                "CONFIG.MAX_SET_VALUE " +
                "FROM OPERATION_CONFIG AS CONFIG, " +
                "BASE_DEVICE AS DEVICE " +
                "WHERE CONFIG.DEVICE_CODE = DEVICE.DEVICE_CODE " +
                "AND DEVICE.DEVICE_CATEGORY_SUB = '" + deviceCategorySub + "'";

        return sqlSession.selectMap(sql, EssVO.ConfigVO.class, "CONFIG_TYPE");
    }

    public List<ScheduleVO> getScheduleList(String currentDate) {
        String sql = "SELECT SCHEDULE_DATE, " +
                "CHARGE_COUNT, " +
                "DISCHARGE_COUNT, " +
                "COMPLETED_CHARGE_COUNT, " +
                "COMPLETED_DISCHARGE_COUNT " +
                "FROM OPERATION_SCHEDULE " +
                "WHERE SCHEDULE_DATE >= '" + currentDate + "'";

        return sqlSession.selectList(sql, ScheduleVO.class);
    }

    public List<ScheduleVO.ScheduleDetailVO> getScheduleDetailList(String scheduleDate, String scheduleTime) {
        String sql = "SELECT SCHEDULE_NO, " +
                "SCHEDULE_START_DATE, " +
                "SCHEDULE_END_DATE, " +
                "SCHEDULE_START_TIME, " +
                "SCHEDULE_END_TIME, " +
                "SCHEDULE_TYPE, " +
                "SCHEDULE_STATUS, " +
                "OPERATION_MODE_TYPE, " +
                "TARGET_UNIT, " +
                "TARGET_AMOUNT, " +
                "RUN_START_DATE_TIME, " +
                "RUN_END_DATE_TIME " +
                "FROM OPERATION_SCHEDULE_DETAIL " +
                "WHERE SCHEDULE_START_DATE = '" + scheduleDate + "' " +
                "AND SCHEDULE_START_TIME >= '" + scheduleTime + "' ";

        return sqlSession.selectList(sql, ScheduleVO.ScheduleDetailVO.class);
    }

    public void updateOperationScheduleCount() {
        
    }
}
