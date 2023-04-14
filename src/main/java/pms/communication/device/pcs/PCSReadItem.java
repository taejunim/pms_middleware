package pms.communication.device.pcs;

import pms.vo.device.PcsVO;

import java.util.ArrayList;
import java.util.List;

public class PCSReadItem {
    private final List<PcsVO.RequestItem> requestItems = new ArrayList<>();

    public PCSReadItem() {
        setRequestItems();
    }

    public List<PcsVO.RequestItem> getRequestItems() {
        return requestItems;
    }

    private void setRequestItems() {
        requestItems.add(setRequestItem("30001", 0, "UINT", 1, "status", "operationStatus"));
        requestItems.add(setRequestItem("30002", 1, "UINT", 1, "status", "operationReadyStatus"));
        requestItems.add(setRequestItem("30003", 2, "UINT", 1, "status", "faultFlag"));
        requestItems.add(setRequestItem("30004", 3, "UINT", 1, "status", "controlStatus"));
        requestItems.add(setRequestItem("30005", 4, "UINT", 1, "status", "operationModeStatus"));
        requestItems.add(setRequestItem("30006", 5, "UINT", 1, "status", "fault1"));
        requestItems.add(setRequestItem("30007", 6, "UINT", 1, "status", "fault2"));
        requestItems.add(setRequestItem("30008", 7, "UINT", 1, "status", "acMainMcStatus"));
        requestItems.add(setRequestItem("30009", 8, "UINT", 1, "status", "dcMainMcStatus"));
        requestItems.add(setRequestItem("3000A", 9, "UINT", 1, "value", "igbtTemperature1"));
        requestItems.add(setRequestItem("3000B", 10, "UINT", 1, "value", "igbtTemperature2"));
        requestItems.add(setRequestItem("3000C", 11, "UINT", 1, "value", "igbtTemperature3"));
        requestItems.add(setRequestItem("3000D", 12, "UINT", 10, "value", "rsLineVoltage"));
        requestItems.add(setRequestItem("3000E", 13, "UINT", 10, "value", "stLineVoltage"));
        requestItems.add(setRequestItem("3000F", 14, "UINT", 10, "value", "trLineVoltage"));
        requestItems.add(setRequestItem("30010", 15, "UINT", 10, "value", "rPhaseCurrent"));
        requestItems.add(setRequestItem("30011", 16, "UINT", 10, "value", "sPhaseCurrent"));
        requestItems.add(setRequestItem("30012", 17, "UINT", 10, "value", "tPhaseCurrent"));
        requestItems.add(setRequestItem("30013", 18, "UINT", 10, "value", "frequency"));
        requestItems.add(setRequestItem("30014", 19, "UINT", 10, "value", "dcLinkVoltage"));
        requestItems.add(setRequestItem("30015", 20, "UINT", 10, "value", "batteryVoltage"));
        requestItems.add(setRequestItem("30016", 21, "INT", 10, "value", "batteryCurrent"));
        requestItems.add(setRequestItem("30017", 22, "UINT", 1, "value", "heartbeat"));
        requestItems.add(setRequestItem("30018", 23, "INT", 10, "value", "outputPower"));
        requestItems.add(setRequestItem("30019", 24, "UINT", 10, "value", "accumulatedChargeEnergy"));
        requestItems.add(setRequestItem("3001A", 25, "UINT", 10, "value", "accumulatedDischargeEnergy"));
        requestItems.add(setRequestItem("3001B", 26, "INT", 1, "value", "referencePower"));
        requestItems.add(setRequestItem("3001C", 27, "UINT", 1, "status", "emergencyStopFlag"));
    }

    private PcsVO.RequestItem setRequestItem(String address, int register, String dataType, int scale, String type, String name) {
        PcsVO.RequestItem requestItem = new PcsVO.RequestItem();

        requestItem.setAddress(address);
        requestItem.setRegister(register);
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
