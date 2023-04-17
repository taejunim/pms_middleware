package pms.vo.device;

import lombok.Data;

@Data
public class SensorVO {
    private String sensorCode;
    private int regDate;
    private String measure1;
    private String measure2;
    private String measure3;
    private String warningFlag;
    private String faultFlag;
}
