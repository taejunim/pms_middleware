package pms.vo.system;

import lombok.Data;

/**
 * packageName    : pms.vo.system
 * fileName       : PowerRelayVO
 * author         : tjlim
 * date           : 2023/07/25
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2023/07/25        tjlim       최초 생성
 */
@Data
public class PowerRelayVO {
    private String relayCode; //Relay 코드
    private int regDate;    //등록 일시
    private String status; //Relay 상태
    private float rsLineVoltage;
    private float stLineVoltage;
    private float trLineVoltage;
    private float rPhaseVoltage;
    private float sPhaseVoltage;
    private float tPhaseVoltage;
    private float rPhaseCurrent;
    private float sPhaseCurrent;
    private float tPhaseCurrent;
    private float rPhaseActivePower;
    private float sPhaseActivePower;
    private float tPhaseActivePower;
    private float totalActivePower;
    private float rPhaseReactivePower;
    private float sPhaseReactivePower;
    private float tPhaseReactivePower;
    private float totalReactivePower;
    private float rPhaseApparentPower;
    private float sPhaseApparentPower;
    private float tPhaseApparentPower;
    private float totalApparentPower;
    private float rPhasePowerFactor;
    private float sPhasePowerFactor;
    private float tPhasePowerFactor;
    private float averagePowerFactor;
    private float frequency;
    private float totalActiveEnergy;
    private float totalReverseActiveEnergy;
    private float totalReactiveEnergy;
    private float totalReverseReactiveEnergy;
    private float rPhaseReversePower;
    private float sPhaseReversePower;
    private float tPhaseReversePower;
    private String overVoltageRelayAction;
    private String underVoltageRelayAction;
    private String overFrequencyRelayAction;
    private String underFrequencyRelayAction;
    private String reversePowerRelayAction;

    @Data
    public static class RequestItem {
        private String address; //레지스터 주소 (HEX 코드)
        private int register;   //레지스터 주소
        private int size;   //데이터 사이즈
        private String dataType;    //데이터 유형
        private int scale;  //값 크기
        private String type;    //레지스터 유형
        private String name;    //항목 명
    }
}
