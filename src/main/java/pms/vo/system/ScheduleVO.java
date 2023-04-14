package pms.vo.system;

import lombok.Data;

@Data
public class ScheduleVO {
    private String scheduleDate;
    private int chargeCount;
    private int dischargeCount;
    private int completedChargeCount;
    private int completedDischargeCount;

    @Data
    public static class ScheduleDetailVO {
        private int scheduleNo;
        private String scheduleStartDate;
        private String scheduleEndDate;
        private String scheduleType;
        private String scheduleStatus;
        private String operationModeType;
        private String targetUnit;
        private float targetAmount;
        private String runStartDateTime;
        private String runEndDateTime;
    }
}
