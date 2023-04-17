package pms.vo.device;

import lombok.Data;

@Data
public class AirConditionerVO {
    private String airConditionerCode;
    private int regDate;
    private String operationStatus;
    private String operationModeStatus;
    private String indoorTemperature;
    private String waringFlag;
    private String faultFlag;
}
