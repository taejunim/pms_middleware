package pms.database.query;

import pms.database.QueryUtil;
import pms.database.SqlSession;
import pms.vo.device.external.EVChargerVO;

import java.util.ArrayList;
import java.util.List;

public class EVChargerQuery {
    private final SqlSession sqlSession = new SqlSession();

    public void insertEVChargerHistory(List<EVChargerVO> evChargers) {
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO HISTORY_CHARGER " +
                "(REQUEST_DATE, REQUEST_TYPE, CHARGER_ID, STATUS, VOLTAGE, CURRENT, START_DATE, END_DATE, ORIGINAL_JSON) " +
                "VALUES "
        );

        List<Object> voList = new ArrayList<>(evChargers);

        String values = QueryUtil.createInsertValues(voList);
        sql.append(values);

        sqlSession.insert(sql.toString());
    }
}
