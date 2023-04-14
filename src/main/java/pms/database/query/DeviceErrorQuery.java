package pms.database.query;

import pms.database.QueryUtil;
import pms.database.SqlSession;
import pms.vo.device.BmsVO;
import pms.vo.device.error.ComponentErrorVO;
import pms.vo.device.error.DeviceErrorVO;
import pms.vo.system.DeviceVO;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Device Error Query
 * <p>
 * - 장비 오류 관련 쿼리 호출
 */
public class DeviceErrorQuery {
    private final SqlSession sqlSession = new SqlSession();

    public List<DeviceVO.ErrorCodeVO> getDeviceErrorCodeList(String category) {
        String sql = "SELECT ERROR_CODE, " +
                "ERROR_CODE_NAME, " +
                "ERROR_TYPE, " +
                "DEVICE_CATEGORY, " +
                "DEVICE_CATEGORY_SUB, " +
                "MANUFACTURER_CODE, " +
                "REFERENCE_CODE " +
                "FROM BASE_DEVICE_ERROR_CODE " +
                "WHERE USE_FLAG = 'Y' " +
                "AND DEVICE_CATEGORY = '" + category + "'";

        return sqlSession.selectList(sql, DeviceVO.ErrorCodeVO.class);
    }

    /**
     * 장비 오류 코드 조회
     * <p>
     * - 장비 분류 코드별 장비 오류 코드
     *
     * @param category 장비 분류 코드
     * @param mapKey   Map Key
     * @return 장비 오류 코드 Map
     */
    public Map<Object, DeviceVO.ErrorCodeVO> getDeviceErrorCodeMap(String category, String mapKey) {
        String sql = "SELECT ERROR_CODE, " +
                "ERROR_CODE_NAME, " +
                "ERROR_TYPE, " +
                "DEVICE_CATEGORY, " +
                "DEVICE_CATEGORY_SUB, " +
                "MANUFACTURER_CODE, " +
                "REFERENCE_CODE " +
                "FROM BASE_DEVICE_ERROR_CODE " +
                "WHERE USE_FLAG = 'Y' " +
                "AND DEVICE_CATEGORY = '" + category + "'";

        return sqlSession.selectMap(sql, DeviceVO.ErrorCodeVO.class, mapKey);
    }

    public Map<Object, DeviceVO.ErrorCodeVO> getDeviceErrorCodeMap(String category, String[] mapKeys) {
        String sql = "SELECT ERROR_CODE, " +
                "ERROR_CODE_NAME, " +
                "ERROR_TYPE, " +
                "DEVICE_CATEGORY, " +
                "DEVICE_CATEGORY_SUB, " +
                "MANUFACTURER_CODE, " +
                "REFERENCE_CODE " +
                "FROM BASE_DEVICE_ERROR_CODE " +
                "WHERE USE_FLAG = 'Y' " +
                "AND DEVICE_CATEGORY = '" + category + "'";

        return sqlSession.selectMap(sql, DeviceVO.ErrorCodeVO.class, mapKeys, "_");
    }

    public int insertDeviceError(List<DeviceErrorVO> deviceErrors) {
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO HISTORY_ERROR_DEVICE " +
                "(ERROR_DATE, DEVICE_CODE, ERROR_CODE) " +
                "VALUES "
        );

        List<Object> voList = new ArrayList<>(deviceErrors);

        String values = QueryUtil.createInsertValues(voList);
        sql.append(values);

        return sqlSession.insert(sql.toString());
    }

    public int insertComponentError(List<ComponentErrorVO> componentErrors) {
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO HISTORY_ERROR_DEVICE_COMPONENT " +
                "(ERROR_DATE, DEVICE_CODE, COMPONENT_NO, ERROR_CODE) " +
                "VALUES "
        );

        List<Object> voList = new ArrayList<>(componentErrors);

        String values = QueryUtil.createInsertValues(voList);
        sql.append(values);

        return sqlSession.insert(sql.toString());
    }
}
