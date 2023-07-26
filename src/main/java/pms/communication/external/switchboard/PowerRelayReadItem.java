package pms.communication.external.switchboard;

import pms.vo.system.PowerRelayVO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * packageName    : pms.communication.external.switchboard
 * fileName       : PowerRelayReadItem
 * author         : tjlim
 * date           : 2023/07/25
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2023/07/25        tjlim       최초 생성
 */
public class PowerRelayReadItem {

    private final Map<String, List<PowerRelayVO.RequestItem>> requestItemsMap = new HashMap<>();

    public PowerRelayReadItem() {
        setRequestItems();
    }

    public Map<String, List<PowerRelayVO.RequestItem>> getRequestItems() {
        return requestItemsMap;
    }

    private void setRequestItems() {
        List<PowerRelayVO.RequestItem> valueItems = new ArrayList<>();
        valueItems.add(setRequestItem("30001", 0, 2,"INT", 100000, "value", "rsLineVoltage"));
        valueItems.add(setRequestItem("30003", 2, 2,"INT", 100000, "value", "stLineVoltage"));
        valueItems.add(setRequestItem("30005", 4, 2,"INT", 100000, "value", "trLineVoltage"));
        valueItems.add(setRequestItem("30007", 6, 2,"INT", 100000, "value", "rPhaseVoltage"));
        valueItems.add(setRequestItem("30009", 8, 2,"INT", 100000, "value", "sPhaseVoltage"));
        valueItems.add(setRequestItem("30011", 10, 2,"INT", 100000, "value", "tPhaseVoltage"));
        valueItems.add(setRequestItem("30013", 12, 2,"INT", 100000, "value", "rPhaseCurrent"));
        valueItems.add(setRequestItem("30015", 14, 2,"INT", 100000, "value", "sPhaseCurrent"));
        valueItems.add(setRequestItem("30017", 16, 2,"INT", 100000, "value", "tPhaseCurrent"));
        valueItems.add(setRequestItem("30019", 18, 2,"INT", 100000, "value", "rPhaseActivePower"));
        valueItems.add(setRequestItem("30021", 20, 2,"INT", 100000, "value", "sPhaseActivePower"));
        valueItems.add(setRequestItem("30023", 22, 2,"INT", 100000, "value", "tPhaseActivePower"));
        valueItems.add(setRequestItem("30025", 24, 2,"INT", 100000, "value", "totalActivePower"));
        valueItems.add(setRequestItem("30027", 26, 2,"INT", 100000, "value", "rPhaseReactivePower"));
        valueItems.add(setRequestItem("30029", 28, 2,"INT", 100000, "value", "sPhaseReactivePower"));
        valueItems.add(setRequestItem("30031", 30, 2,"INT", 100000, "value", "tPhaseReactivePower"));
        valueItems.add(setRequestItem("30033", 32, 2,"INT", 100000, "value", "totalReactivePower"));
        valueItems.add(setRequestItem("30035", 34, 2,"INT", 100000, "value", "rPhaseApparentPower"));
        valueItems.add(setRequestItem("30037", 36, 2,"INT", 100000, "value", "sPhaseApparentPower"));
        valueItems.add(setRequestItem("30039", 38, 2,"INT", 100000, "value", "tPhaseApparentPower"));
        valueItems.add(setRequestItem("30041", 40, 2,"INT", 100000, "value", "totalApparentPower"));
        valueItems.add(setRequestItem("30043", 42, 2,"INT", 100000, "value", "rPhasePowerFactor"));
        valueItems.add(setRequestItem("30045", 44, 2,"INT", 100000, "value", "sPhasePowerFactor"));
        valueItems.add(setRequestItem("30047", 46, 2,"INT", 100000, "value", "tPhasePowerFactor"));
        valueItems.add(setRequestItem("30049", 48, 2,"INT", 100000, "value", "averagePowerFactor"));
        valueItems.add(setRequestItem("30051", 50, 2,"INT", 100000, "value", "frequency"));
        valueItems.add(setRequestItem("30053", 52, 2,"INT", 100000, "value", "totalActiveEnergy"));
        valueItems.add(setRequestItem("30055", 54, 2,"INT", 100000, "value", "totalReverseActiveEnergy"));
        valueItems.add(setRequestItem("30057", 56, 2,"INT", 100000, "value", "totalReactiveEnergy"));
        valueItems.add(setRequestItem("30059", 58, 2,"INT", 100000, "value", "totalReverseReactiveEnergy"));
        valueItems.add(setRequestItem("30061", 60, 2,"INT", 100000, "value", "maxRsLineVoltage"));
        valueItems.add(setRequestItem("30063", 62, 2,"INT", 100000, "value", "maxStLineVoltage"));
        valueItems.add(setRequestItem("30065", 64, 2,"INT", 100000, "value", "maxTrLineVoltage"));
        valueItems.add(setRequestItem("30067", 66, 2,"INT", 100000, "value", "maxRPhaseVoltage"));
        valueItems.add(setRequestItem("30069", 68, 2,"INT", 100000, "value", "maxSPhaseVoltage"));
        valueItems.add(setRequestItem("30071", 70, 2,"INT", 100000, "value", "maxTPhaseVoltage"));
        valueItems.add(setRequestItem("30073", 72, 2,"INT", 100000, "value", "maxRPhaseCurrent"));
        valueItems.add(setRequestItem("30075", 74, 2,"INT", 100000, "value", "maxSPhaseCurrent"));
        valueItems.add(setRequestItem("30077", 76, 2,"INT", 100000, "value", "maxTPhaseCurrent"));
        valueItems.add(setRequestItem("30079", 78, 2,"INT", 100000, "value", "maxPower"));
        valueItems.add(setRequestItem("30081", 80, 2,"INT", 100000, "value", "p1Angle"));
        valueItems.add(setRequestItem("30083", 82, 2,"INT", 100000, "value", "p2Angle"));
        valueItems.add(setRequestItem("30085", 84, 2,"INT", 100000, "value", "p3Angle"));
        valueItems.add(setRequestItem("30087", 86, 2,"INT", 100000, "value", "rPhaseReversePower"));
        valueItems.add(setRequestItem("30089", 88, 2,"INT", 100000, "value", "sPhaseReversePower"));
        valueItems.add(setRequestItem("30091", 90, 2,"INT", 100000, "value", "tPhaseReversePower"));
        requestItemsMap.put("valueItems", valueItems);

        List<PowerRelayVO.RequestItem> statusItems = new ArrayList<>();
        statusItems.add(setRequestItem("30132", 131, 1,"INT", 1, "status", "overVoltageRelayAction"));
        statusItems.add(setRequestItem("30133", 132, 1,"INT", 1, "status", "underVoltageRelayAction"));
        statusItems.add(setRequestItem("30134", 133, 1,"INT", 1, "status", "overFrequencyRelayAction"));
        statusItems.add(setRequestItem("30135", 134, 1,"INT", 1, "status", "underFrequencyRelayAction"));
        statusItems.add(setRequestItem("30136", 135, 1,"INT", 1, "status", "reversePowerRelayAction"));
        requestItemsMap.put("statusItems", statusItems);
    }

    private PowerRelayVO.RequestItem setRequestItem(String address, int register, int size, String dataType, int scale, String type, String name) {
        PowerRelayVO.RequestItem requestItem = new PowerRelayVO.RequestItem();

        requestItem.setAddress(address);
        requestItem.setRegister(register);
        requestItem.setSize(size);
        requestItem.setDataType(dataType);
        requestItem.setScale(scale);
        requestItem.setType(type);
        requestItem.setName(name);

        return requestItem;
    }

    public static String[] toBits(int value) {
        String binary = Integer.toBinaryString(value);
        String bitString = String.format("%16s", binary).replace(" ", "0");

        return bitString.split("");
    }
}
