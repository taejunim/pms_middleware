package pms.vo.device.external;

import com.google.gson.JsonObject;
import lombok.Data;

@Data
public class EVChargerVO {
    private int requestDate;
    private String requestType;
    private String chargerId;
    private String status;
    private float voltage;
    private float current;
    private String startDate;
    private String endDate;
    private String originalJson;
}
