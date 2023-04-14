package pms.vo.history;

import lombok.Data;

@Data
public class ControlHistoryVO {
    private String controlCode; //제어 코드
    private int controlDate;    //제어 일시
    private String controlRequestType;  //제어 이력 구분
    private String controlRequestDetailType;    //제어 이력 상세 구분
    private int controlRequestValue;
    private String referenceCode;   //참조 코드
    private String controlCompleteFlag; //제어 성공 여부
    private int deviceResponseDate; //장비 응답 일시
    private String controlRequestId;    //장비 응답 일시
}
