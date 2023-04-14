package pms.vo.system;

import lombok.Data;

import java.math.BigDecimal;

/**
 * ESS 정보
 */
@Data
public class EssVO {
    private String essCode; //ESS 코드
    private String essType; //ESS 유형 - 01: 고정형 ESS, 02: 이동형 ESS
    private String totalCharge; //총 누적 충전량
    private String totalDischarge;  //총 누적 방전량
    private String autoControlFlag; //자동 제어 여부 - Y: 자동(일정), N: 수동(원격)

    /**
     * ESS 환경설정 정보
     */
    @Data
    public static class ConfigVO {
        private String configCode;  //설정 코드
        private String deviceCategorySub;
        private String deviceCode;  //장비 코드
        private String configType;  //설정 구분
        private BigDecimal minSetValue; //최소 설정값
        private BigDecimal maxSetValue; //최대 설정값
    }
}
