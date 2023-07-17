package pms.vo.device;

import lombok.Data;

import java.util.List;

/**
 * Converter VO
 * <p>
 * - 컨버터 정보 (이동형 ESS - PCS)
 */
@Data
public class ConverterVO {
    private ACConverterVO acConverter;
    private List<ACInverterVO> acInverters;
    private DCConverterVO dcConverter;
    private List<DCInverterVO> dcInverters;

    /**
     * AC/DC 컨버터 VO
     */
    @Data
    public static class ACConverterVO {
        private String converterCode;   //컨버터 코드
        private int regDate;    //등록 일시
        private String operationStatus; //운전 상태
        private String operationModeStatus; //운전 모드 상태
        private String setOperationMode;    //설정 운전 모드
        private float setCurrent;   //설정 전류 값
        private float totalActiveCurrent;   //총 유효 전류
        private float totalVoltage; //총 전압
        private float totalPower;   //총 전력
        private float internalTemp; //함체 내부 온도
        private float transformerTemp;  //교류 변압기 온도
        private String warningFlag; //경고 여부
        private String faultFlag;   //결함 여부
    }

    /**
     * AC/DC 인버터 VO
     */
    @Data
    public static class ACInverterVO {
        private String converterCode;   //컨버터 코드
        private int regDate;    //등록 일시
        private int inverterNo; //인버터 번호
        private String modeStatus;  //모드 상태
        private String inverterStatus;  //인버터 상태
        private float power;    //전력
        private float totalCurrent; //총 전류
        private float outputVoltage;    //출력 전압
        private float outputFrequency;  //출력 주파수
        private float gridVoltage;  //계통 라인 전압
        private float gridFrequency;    //계통 라인 주파수
        private int gridPhaseDifference;    //계통 & 인버터 위상 차
        private float powerFactor;  //역률
        private float acCurrent;    //교류 전류
        private float dcVoltage;    //직류 전압
        private float dcOffset; //직류 성분
        private float activeCurrent;    //유효 전류
        private float activeCurrentContrast;    //유효 전류(정격 대비)
        private float reactiveCurrentContrast;  //무효 전류(정격 대비)
        private float stackTemp;    //인버터 스택 온도
        private float inductor1Temp;    //필터 인덕터 1. 온도
        private float inductor2Temp;    //필터 인덕터 2. 온도
        private float capacitorTemp;    //필터 커패시터 온도
        private String warningId;   //경고 ID
        private String faultId; //결함 ID
        private String warningFlag; //경고 여부
        private String faultFlag;   //결함 여부
    }

    /**
     * DC/AC 컨버터 VO
     */
    @Data
    public static class DCConverterVO {
        private String converterCode;   //컨버터 코드
        private int regDate;    //등록 일시
        private String operationStatus; //운전 상태
        private float totalDcPower; //총 직류 전력
        private float totalCurrent; //총 전류
        private float convertDcPower;   //변환 직류 전력
        private float dcCurrent;    //직류 전류
        private float internalTemp; //함체 내부 온도
        private String warningFlag; //경고 여부
        private String faultFlag;   //결함 여부
    }

    /**
     * DC/DC 인버터 VO
     */
    @Data
    public static class DCInverterVO {
        private String converterCode;   //컨버터 코드
        private int regDate;    //등록 일시
        private int inverterNo; //인버터 번호
        private String modeStatus;  //모드 상태
        private String inverterStatus;  //인버터 상태
        private float power;    //전력
        private float current;  //전류
        private float voltage;  //전압
        private float dcPower;  //직류 전력
        private float dcCurrent;    //직류 전류
        private float activeCurrentContrast;    //유효 전류(정격 대비)
        private float refActiveCurrentPercentage;   //지령 유효 전류(백분율)
        private float stackTemp;    //인버터 스택 온도
        private float inductorTemp; //필터 인덕터 온도
        private float capacitorTemp;    //필터 커패시터 온도
        private String warningId;   //경고 ID
        private String faultId; //결함 ID
        private String warningFlag; //경고 여부
        private String faultFlag;   //결함 여부
    }

    /**
     * 수신 요청 항목 VO
     */
    @Data
    public static class RequestItem {
        private String functionCode;    //명령어 집합코드
        private String address; //항목 주소
        private String dataType;    //데이터 유형
        private int size;   //데이터 사이즈 (WORD 단위)
        private int scale;  //항목 값 크기
        private String type;    //항목 유형
        private String group;   //항목 그룹
        private String name;    //항목 명
    }
}
