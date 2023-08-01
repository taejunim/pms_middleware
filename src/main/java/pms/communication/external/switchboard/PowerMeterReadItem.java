package pms.communication.external.switchboard;

import pms.vo.system.PowerMeterVO;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * packageName    : pms.communication.external.switchboard
 * fileName       : PowerMeterReadItem
 * author         : youyeong
 * date           : 2023/07/28
 * description    : EV충전기 전력 계측기 통신 ReadItem
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2023/07/28        youyeong       최초 생성
 */
public class PowerMeterReadItem {
    private final Map<String, List<PowerMeterVO.RequestItem>> requestItemsMap = new HashMap<>();

    public PowerMeterReadItem() {
        setRequestItems();
    }

    public Map<String, List<PowerMeterVO.RequestItem>> getRequestItems() {
        return requestItemsMap;
    }

    private void setRequestItems() {

        //Current
        List<PowerMeterVO.RequestItem> currentItem = new ArrayList<>();
        currentItem.add(setRequestItem("3000",2999,2,"FLOAT32",1,"currentA"));
        currentItem.add(setRequestItem("3002",3001,2,"FLOAT32",1,"currentB"));
        currentItem.add(setRequestItem("3004",3003,2,"FLOAT32",1,"currentC"));
        currentItem.add(setRequestItem("3006",3005,2,"FLOAT32",1,"currentN"));
        currentItem.add(setRequestItem("3008",3007,2,"FLOAT32",1,"currentG"));
        currentItem.add(setRequestItem("3010",3009,2,"FLOAT32",1,"currentAvg"));
        requestItemsMap.put("currentItem", currentItem);

        //Voltage
        List<PowerMeterVO.RequestItem> voltageItem = new ArrayList<>();
        voltageItem.add(setRequestItem("3020",3019,2,"FLOAT32",1,"voltageAB"));
        voltageItem.add(setRequestItem("3022",3021,2,"FLOAT32",1,"voltageBC"));
        voltageItem.add(setRequestItem("3024",3023,2,"FLOAT32",1,"voltageCA"));
        voltageItem.add(setRequestItem("3026",3025,2,"FLOAT32",1,"voltageLLAvg"));
        voltageItem.add(setRequestItem("3028",3027,2,"FLOAT32",1,"voltageAN"));
        voltageItem.add(setRequestItem("3030",3029,2,"FLOAT32",1,"voltageBN"));
        voltageItem.add(setRequestItem("3032",3031,2,"FLOAT32",1,"voltageCN"));
        voltageItem.add(setRequestItem("3036",3035,2,"FLOAT32",1,"voltageLNAvg"));
        requestItemsMap.put("voltageItem", voltageItem);

        //Power
        List<PowerMeterVO.RequestItem> powerItem = new ArrayList<>();
        powerItem.add(setRequestItem("3054",3053,2,"FLOAT32",1,"activePowerA"));
        powerItem.add(setRequestItem("3056",3055,2,"FLOAT32",1,"activePowerB"));
        powerItem.add(setRequestItem("3058",3057,2,"FLOAT32",1,"activePowerC"));
        powerItem.add(setRequestItem("3060",3059,2,"FLOAT32",1,"activePowerTotal"));
        powerItem.add(setRequestItem("3062",3061,2,"FLOAT32",1,"reactivePowerA"));
        powerItem.add(setRequestItem("3064",3063,2,"FLOAT32",1,"reactivePowerB"));
        powerItem.add(setRequestItem("3066",3065,2,"FLOAT32",1,"reactivePowerC"));
        powerItem.add(setRequestItem("3068",3067,2,"FLOAT32",1,"reactivePowerTotal"));
        powerItem.add(setRequestItem("3070",3069,2,"FLOAT32",1,"apparentPowerA"));
        powerItem.add(setRequestItem("3072",3071,2,"FLOAT32",1,"apparentPowerB"));
        powerItem.add(setRequestItem("3074",3073,2,"FLOAT32",1,"apparentPowerC"));
        powerItem.add(setRequestItem("3076",3075,2,"FLOAT32",1,"apparentPowerTotal"));
        requestItemsMap.put("powerItem", powerItem);

        //Power Factor
        List<PowerMeterVO.RequestItem> powerFactorItem = new ArrayList<>();
        powerFactorItem.add(setRequestItem("3078",3077,2,"FLOAT32",1,"powerFactorA"));
        powerFactorItem.add(setRequestItem("3080",3079,2,"FLOAT32",1,"powerFactorB"));
        powerFactorItem.add(setRequestItem("3082",3081,2,"FLOAT32",1,"powerFactorC"));
        powerFactorItem.add(setRequestItem("3084",3083,2,"FLOAT32",1,"powerFactorTotal"));
        requestItemsMap.put("powerFactorItem", powerFactorItem);

        //Frequency
        List<PowerMeterVO.RequestItem> frequencyItem = new ArrayList<>();
        frequencyItem.add(setRequestItem("3110",3109,2,"FLOAT32",1,"frequency"));
        requestItemsMap.put("frequencyItem", frequencyItem);

        //Energy
        List<PowerMeterVO.RequestItem> energyItem = new ArrayList<>();
        energyItem.add(setRequestItem("3204",3203,4,"INT64",1,"activeEnergyDelivered"));
        energyItem.add(setRequestItem("3208",3207,4,"INT64",1,"activeEnergyReceived"));
        energyItem.add(setRequestItem("3220",3219,4,"INT64",1,"reactiveEnergyDelivered"));
        energyItem.add(setRequestItem("3224",3223,4,"INT64",1,"reactiveEnergyReceived"));
        energyItem.add(setRequestItem("3236",3235,4,"INT64",1,"apparentEnergyDelivered"));
        energyItem.add(setRequestItem("3240",3239,4,"INT64",1,"apparentEnergyReceived"));
        requestItemsMap.put("energyItem", energyItem);

    }

    private PowerMeterVO.RequestItem setRequestItem(String address, int register, int size, String dataType, int scale,  String name) {
        PowerMeterVO.RequestItem requestItem = new PowerMeterVO.RequestItem();

        requestItem.setAddress(address);
        requestItem.setRegister(register);
        requestItem.setSize(size);
        requestItem.setDataType(dataType);
        requestItem.setScale(scale);
        requestItem.setName(name);

        return requestItem;
    }

}
