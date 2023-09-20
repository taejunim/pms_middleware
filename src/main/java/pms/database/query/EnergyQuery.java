package pms.database.query;

import pms.database.QueryUtil;
import pms.database.SqlSession;
import pms.vo.history.EnergyDetailHistoryVO;
import pms.vo.history.EnergyHistoryVO;
import pms.vo.history.EnergyHistoryVONew;

public class EnergyQuery {
    SqlSession sqlSession = new SqlSession();

    public int insertEnergyHistory(EnergyHistoryVO energyHistoryVO) {
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO HISTORY_ENERGY " +
                "(ENERGY_NO, DEVICE_CODE, OPERATION_MODE_TYPE, OPERATION_TYPE, OPERATION_HISTORY_TYPE, " +
                "SCHEDULE_OPERATION_FLAG, SCHEDULE_NO, FINAL_ACCUMULATED_ENERGY, START_DATE, END_DATE, UPDATED_AT) " +
                "VALUES "
        );

        String values = QueryUtil.createInsertValue(energyHistoryVO);
        sql.append(values);

        return sqlSession.insert(sql.toString());
    }

    public int updateEnergyHistory(EnergyHistoryVO energyHistoryVO) {
        String sql = "UPDATE HISTORY_ENERGY " +
                "SET " +
                "OPERATION_HISTORY_TYPE = '" + energyHistoryVO.getOperationHistoryType() + "', " +
                "FINAL_ACCUMULATED_ENERGY = " + energyHistoryVO.getFinalAccumulatedEnergy() + ", " +
                "END_DATE = '" + energyHistoryVO.getEndDate() + "', " +
                "UPDATED_AT = '" + energyHistoryVO.getUpdatedAt()  + "' " +
                "WHERE ENERGY_NO = '" + energyHistoryVO.getEnergyNo() + "'";

        return sqlSession.update(sql);
    }


    /**
     * 전력량 이력 등록
     *
     * @param energyHistoryVO
     * @return
     */
    public int insertEnergyHistoryNew(EnergyHistoryVONew energyHistoryVO) {
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO HISTORY_ENERGY " +
                "(ENERGY_NO, OPERATION_HISTORY_TYPE, OPERATION_HISTORY_DATE, PCS_CODE, " +
                "OPERATION_MODE, OPERATION_TYPE, SCHEDULE_NO) " +
                "VALUES "
        );

        String values = QueryUtil.createInsertValue(energyHistoryVO);
        sql.append(values);

        return sqlSession.insert(sql.toString());
    }

    public int insertEnergyDetailHistory(EnergyDetailHistoryVO energyDetailHistoryVO) {
        String sql = "INSERT INTO HISTORY_ENERGY_DETAIL " +
                "(ENERGY_NO, ENERGY_SEQ, ACCUMULATED_ENERGY) " +
                "VALUES " +
                "('" + energyDetailHistoryVO.getEnergyNo() + "', " +
                "(SELECT IFNULL((MAX(ENERGY_SEQ) + 1 ), 1) ENERGY_SEQ " +
                "FROM HISTORY_ENERGY_DETAIL DETAIL " +
                "WHERE ENERGY_NO = '" + energyDetailHistoryVO.getEnergyNo() + "'), " +
                energyDetailHistoryVO.getAccumulatedEnergy() + ")";

        return sqlSession.insert(sql);
    }
}
