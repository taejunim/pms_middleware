package pms.vo.device.control;

import lombok.Data;

@Data
public class ControlRequestVO {
    private String remoteId;    //원격 ID - 제어 요청 구분이 '02: 원격(수동)'인 경우에만 사용, 원격 제어 시 웹소켓 응답용
    private String type;    //제어
    private String detailType;
    private int date;
    private int address;
    private String deviceCode;
    private String controlType;
    private String controlCode;
    private int controlValue;
    private String referenceCode;
}
