package pms.vo.system;

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

    private int    regDate;
    private String meterCode;

    private float currentA;
    private float currentB;
    private float currentC;
    private float currentN;
    private float currentG;
    private float currentAvg;

    private float voltageAB;
    private float voltageBC;
    private float voltageCA;
    private float voltageLLAvg;
    private float voltageAN;
    private float voltageBN;
    private float voltageCN;
    private float voltageLNAvg;

    private float activePowerA;
    private float activePowerB;
    private float activePowerC;
    private float activePowerTotal;
    private float reactivePowerA;
    private float reactivePowerB;
    private float reactivePowerC;
    private float reactivePowerTotal;
    private float apparentPowerA;
    private float apparentPowerB;
    private float apparentPowerC;
    private float apparentPowerTotal;

    private float powerFactorA;
    private float powerFactorB;
    private float powerFactorC;
    private float powerFactorTotal;
    private float displacementPowerFactorA;
    private float displacementPowerFactorB;
    private float displacementPowerFactorC;
    private float displacementPowerFactorTotal;

    private float frequency;

    private long activeEnergyDelivered;
    private long activeEnergyReceived;
    private long activeEnergyDeliveredPlusReceived;
    private long activeEnergyDeliveredMinusReceived;
    private long reactiveEnergyDelivered;
    private long reactiveEnergyReceived;
    private long reactiveEnergyDeliveredPlusReceived;
    private long reactiveEnergyDeliveredMinusReceived;
    private long apparentEnergyDelivered;
    private long apparentEnergyReceived;
    private long apparentEnergyDeliveredPlusReceived;
    private long apparentEnergyDeliveredMinusReceived;

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
