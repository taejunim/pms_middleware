package pms.vo.system;

import lombok.Data;

/**
 * 장비 정보
 */
@Data
public class DeviceVO {
    private String deviceCode;  //장비 코드
    private int deviceNo;    //장비 번호
    private String deviceName;  //장비 명
    private String deviceCategory;  //장비 분류
    private String deviceCategorySub;   //장비 하위 분류
    private String deviceRoom;  //장비 실 코드

    /**
     * 장비 구성요소 정보
     */
    @Data
    public static class ComponentVO {
        private String deviceCode;  //장비 코드
        private int componentNo;    //구성요소 번호
        private String componentName;   //구성요소 명
    }

    /**
     * 장비 제어 정보
     */
    @Data
    public static class ControlVO {
        private String controlCode; //제어 코드
        private String deviceCode;  //장비 코드
        private String controlType; //제어 구분
        private int controlValue;    //제어 값
    }

    /**
     * 장비 오류 코드 정보
     */
    @Data
    public static class ErrorCodeVO {
        private String errorCode;   //오류 코드
        private String errorCodeName;   //오류 코드 명
        private String errorType;   //오류 구분
        private String deviceCategory;  //장비 분류
        private String deviceCategorySub;   //장비 하위 분류
        private String manufacturerCode;    //제조사 개별 오류 코드
        private String referenceCode;   //참고 코드
    }
}
