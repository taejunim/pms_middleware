package pms.vo.history;

import lombok.Data;

@Data
public class EnergyDetailHistoryVO {
    private String energyNo;
    private int energySeq;
    private float accumulatedEnergy;
}
