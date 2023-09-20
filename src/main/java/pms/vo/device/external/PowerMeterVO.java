package pms.vo.device.external;

import lombok.Data;
/**
 * packageName    : pms.vo.system
 * fileName       : PowerMeterVO
 * author         : youyeong
 * date           : 2023/07/28
 * description    : EV충전기 전력계측기 통신 VO
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2023/07/28        youyeong       최초 생성
 */
@Data
public class PowerMeterVO {
    private String meterCode;
    private int    regDate;
    private String status;                      //Meter 상태
    private float rPhaseCurrent;
    private float sPhaseCurrent;
    private float tPhaseCurrent;
    private float nPhaseCurrent;
    private float gPhaseCurrent;
    private float averagePhaseCurrent;
    private float rsLineVoltage;
    private float stLineVoltage;
    private float trLineVoltage;
    private float averageLineVoltage;
    private float rPhaseVoltage;
    private float sPhaseVoltage;
    private float tPhaseVoltage;
    private float averagePhaseVoltage;
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
    private float totalPowerFactor;
    private float frequency;
    private long deliveredActiveEnergy;
    private long receivedActiveEnergy;
    private long deliveredReactiveEnergy;
    private long receivedReactiveEnergy;
    private long deliveredApparentEnergy;
    private long receivedApparentEnergy;

    @Data
    public static class RequestItem {
        private String address;     //레지스터 주소
        private int register;       //레지스터 주소
        private int size;           //데이터 사이즈
        private String dataType;    //데이터 유형
        private int scale;          //값 크기
        private String name;        //항목 명
    }
}
