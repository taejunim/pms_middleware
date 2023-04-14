package pms.vo.history;

import lombok.Data;

@Data
public class EnergyHistoryVO {
    private String energyNo;
    private String deviceCode;
    private String operationModeType;
    private String operationType;
    private String operationHistoryType;
    private String schedulerOperationFlag;
    private String scheduleNo;
    private float finalAccumulatedEnergy;
    private int startDate;
    private int endDate;
    private String updatedAt;
}
