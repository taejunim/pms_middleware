package pms.vo.device.control;

import lombok.Data;

@Data
public class ControlRequestVO {
    private String remoteId;    //원격 ID - 제어 요청 구분이 '02: 원격(수동)'인 경우에만 사용, 원격 제어 시 웹소켓 응답용
    private String type;    //제어 요청 구분
    private String detailType;  //제어 요청 상세 구분
    private int date;   //제어 일시
    private int address;    //제어 주소
    private String deviceCode;  //장비 코드
    private String controlType; //제어 구분
    private String controlCode; //제어 코드
    private int controlValue;   //제어 값
    private String controllerId;    //제어자 ID
    private String referenceCode;   //참조 코드 - 제어 요청 구분(type) '03: 환경설정(자동)'인 경우
}
