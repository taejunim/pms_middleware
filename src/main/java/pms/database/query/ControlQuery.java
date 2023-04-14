package pms.database.query;

import pms.database.QueryUtil;
import pms.database.SqlSession;
import pms.vo.device.PcsVO;
import pms.vo.history.ControlHistoryVO;
import pms.vo.system.DeviceVO;

import java.sql.SQLException;
import java.util.Map;

public class ControlQuery {
    SqlSession sqlSession = new SqlSession();

    public Map<Object, DeviceVO.ControlVO> getControlCodes(String mapKey) {
        String sql = "SELECT CONTROL_CODE, " +
                "DEVICE_CODE, " +
                "CONTROL_TYPE, " +
                "CONTROL_VALUE " +
                "FROM BASE_DEVICE_CONTROL " +
                "WHERE USE_FLAG = 'Y'";

        return sqlSession.selectMap(sql, DeviceVO.ControlVO.class, mapKey);
    }

    public int insertControlHistory(ControlHistoryVO controlHistoryVO) {
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO HISTORY_CONTROL " +
                "(CONTROL_CODE, CONTROL_DATE, " +
                "CONTROL_REQUEST_TYPE, CONTROL_REQUEST_DETAIL_TYPE, CONTROL_REQUEST_VALUE, REFERENCE_CODE, " +
                "CONTROL_COMPLETE_FLAG, DEVICE_RESPONSE_DATE, CONTROL_REQUEST_ID) " +
                "VALUES "
        );

        String values = QueryUtil.createInsertValue(controlHistoryVO);
        sql.append(values);

        return sqlSession.insert(sql.toString());
    }
}
