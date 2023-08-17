package pms.vo.device.error;

import lombok.Data;

@Data
public class ComponentErrorVO {
    private int errorDate;  //오류 발생 일시
    private String deviceCode;  //장비 코드
    private int componentNo;    //구성요소 번호
    private String errorCode;   //오류 코드
    private String remarks; //비고
}
