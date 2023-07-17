package pms.communication.device.bms;

import com.ghgande.j2mod.modbus.procimg.InputRegister;
import com.ghgande.j2mod.modbus.procimg.SimpleInputRegister;
import pms.vo.device.BmsVO;
import pms.vo.system.DeviceVO;
import pms.vo.system.PmsVO;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BMSReadItem {
    private final List<DeviceVO.ComponentVO> modules;
    private final Map<String, List<BmsVO.RequestItem>> requestItemsMap = new HashMap<>();

    public BMSReadItem(String rackCode) {
        this.modules = PmsVO.modules.get(rackCode);

        setRackItems();
        setModuleItems();
        setCellItems();
    }

    public Map<String, List<BmsVO.RequestItem>> getRequestItems() {
        return requestItemsMap;
    }

    private void setRackItems() {
        List<BmsVO.RequestItem> rackItems1 = new ArrayList<>();
        rackItems1.add(setRequestItem("30001", 1, 2, "FLOAT", 10, "value", 0, "userSoc"));
        rackItems1.add(setRequestItem("30003", 3, 2, "FLOAT", 10, "value", 0, "realSoc"));
        rackItems1.add(setRequestItem("30005", 5, 2, "FLOAT", 100, "value", 0, "voltage"));
        requestItemsMap.put("rack-items-1", rackItems1);

        List<BmsVO.RequestItem> rackItems2 = new ArrayList<>();
        rackItems2.add(setRequestItem("30367", 871, 2, "FLOAT", 10, "value", 0, "currentSensor1"));
        rackItems2.add(setRequestItem("30369", 873, 2, "FLOAT", 10, "value", 0, "currentSensor2"));
        rackItems2.add(setRequestItem("3036B", 875, 2, "FLOAT", 1, "value", 0, "positiveVoltageResistance"));
        rackItems2.add(setRequestItem("3036D", 877, 2, "FLOAT", 1, "value", 0, "negativeVoltageResistance"));
        rackItems2.add(setRequestItem("3036F", 879, 2, "FLOAT", 10, "value", 0, "soh"));
        rackItems2.add(setRequestItem("30371", 881, 1, "UINT", 1, "value", 0, "chargeCurrentLimit"));
        rackItems2.add(setRequestItem("30372", 882, 1, "UINT", 1, "value", 0, "dischargeCurrentLimit"));
        rackItems2.add(setRequestItem("30373", 883, 1, "UINT", 1, "value", 0, "chargePowerLimit"));
        rackItems2.add(setRequestItem("30374", 884, 1, "UINT", 1, "value", 0, "dischargePowerLimit"));
        requestItemsMap.put("rack-items-2", rackItems2);

        List<BmsVO.RequestItem> rackItems3 = new ArrayList<>();
        rackItems3.add(setRequestItem("34001", 16385, 1, "UINT", 1, "status", 0, "errorLevel1"));
        rackItems3.add(setRequestItem("34002", 16386, 1, "UINT", 1, "status", 0, "errorLevel2"));
        rackItems3.add(setRequestItem("34003", 16387, 1, "UINT", 1, "status", 0, "systemStatus"));
        rackItems3.add(setRequestItem("34004", 16388, 1, "UINT", 1, "status", 0, "rackFault1"));
        rackItems3.add(setRequestItem("34005", 16389, 1, "UINT", 1, "status", 0, "rackFault2"));
        requestItemsMap.put("rack-status-items", rackItems3);
    }

    private void setModuleItems() {
        List<BmsVO.RequestItem> moduleVoltItems = getModuleItems(7, 2, "FLOAT", 100, "value", "moduleVoltage");
        requestItemsMap.put("module-volt-items", moduleVoltItems);

        List<BmsVO.RequestItem> moduleBalanceItems = getModuleItems(839, 1, "UINT", 1, "status", "balancingStatus");
        requestItemsMap.put("module-balancing-items", moduleBalanceItems);

        List<BmsVO.RequestItem> moduleFaultItems = getModuleItems(16390, 1, "UINT", 1, "status", "moduleFault");
        requestItemsMap.put("module-fault-items", moduleFaultItems);
    }

    private void setCellItems() {
        List<BmsVO.RequestItem> cellVoltItems1 = getCellItems(1, 5, 71, 100, "voltage");
        requestItemsMap.put("cell-volt-items-1", cellVoltItems1);

        List<BmsVO.RequestItem> cellVoltItems2 = getCellItems(6, 5, 151, 100, "voltage");
        requestItemsMap.put("cell-volt-items-2", cellVoltItems2);

        List<BmsVO.RequestItem> cellVoltItems3 = getCellItems(11, 5, 231, 100, "voltage");
        requestItemsMap.put("cell-volt-items-3", cellVoltItems3);

        List<BmsVO.RequestItem> cellTempItems = getCellItems(1, 15, 583, 10, "temperature");
        requestItemsMap.put("cell-temp-items", cellTempItems);
    }

    private BmsVO.RequestItem setRequestItem(String address, int register, int size, String dataType, int scale, String type, int no, String name) {
        BmsVO.RequestItem requestItem = new BmsVO.RequestItem();

        requestItem.setAddress(address);
        requestItem.setRegister(register);
        requestItem.setSize(size);
        requestItem.setDataType(dataType);
        requestItem.setScale(scale);
        requestItem.setType(type);
        requestItem.setNo(no);
        requestItem.setName(name);

        return requestItem;
    }

    private List<BmsVO.RequestItem> getModuleItems(int startRegister, int size, String dataType, int scale, String type, String name) {
        int register = startRegister;

        List<BmsVO.RequestItem> moduleItems = new ArrayList<>();
        BmsVO.RequestItem requestItem;

        for (DeviceVO.ComponentVO componentVO : modules) {
            int moduleNo = componentVO.getComponentNo();
            String address = toHexAddress(register);

            requestItem = setRequestItem(address, register, size, dataType, scale, type, moduleNo, name);
            moduleItems.add(requestItem);

            register += size;
        }

        return moduleItems;
    }

    private List<BmsVO.RequestItem> getCellItems(int startModuleNo, int repeat, int startRegister, int scale, String group) {
        int cellCount = 16;
        int register = startRegister;

        if (group.equals("temperature")) {
            cellCount = cellCount / 2;
        }

        List<BmsVO.RequestItem> cellItems = new ArrayList<>();
        BmsVO.RequestItem requestItem;

        for (int i = 0; i < repeat; i++) {
            String address = toHexAddress(register);
            String itemName = null;
            String convertName = group.substring(0, 1).toUpperCase() + group.substring(1);

            for (int j = 1; j <= cellCount; j++) {
                switch (group) {
                    case "voltage":
                        itemName = "cell" + j + convertName;
                        break;
                    case "temperature":
                        itemName = "cell" + convertName + j;
                        break;
                }

                requestItem = setRequestItem(address, register, 1, "UINT", scale, "value", startModuleNo + i, itemName);
                cellItems.add(requestItem);

                register += 1;
            }
        }

        return cellItems;
    }

    private String toHexAddress(int register) {
        String hex = Integer.toHexString(register);

        return String.format("%5s", hex.toUpperCase())
                .replace(" ", "0")
                .replaceFirst("0", "3");
    }

    public static InputRegister toLittleEndian(InputRegister inputRegister) {
        int registerValue = (inputRegister.toBytes()[1] << 8) & 0xFF00 | (inputRegister.toBytes()[0]) & 0xFF;

        return new SimpleInputRegister(registerValue);
    }

    private static float toFloat(byte[] lowBytes, byte[] highBytes, int scale) {
        byte[] bytes = new byte[4];
        bytes[0] = highBytes[0];
        bytes[1] = highBytes[1];
        bytes[2] = lowBytes[0];
        bytes[3] = lowBytes[1];

        int value = 0;
        value |= (bytes[0] << 24) & 0xFF000000;
        value |= (bytes[1] << 16) & 0xFF0000;
        value |= (bytes[2] << 8) & 0xFF00;
        value |= (bytes[3]) & 0xFF;

        float result = Float.intBitsToFloat(value);

        if (value > 1259902592) {
            result = 10000000;
        }

        result = Math.round(result * scale) / (float) scale;

        return result;
    }

    public static String[] toBits(int value) {
        String binary = Integer.toBinaryString(value);
        String bitString = String.format("%16s", binary).replace(" ", "0");

        return bitString.split("");
    }

    public static Object getFieldValue(BmsVO.ResponseItem responseItem, Field field) {
        String dataType = responseItem.getDataType();
        int scale = responseItem.getScale();
        List<InputRegister> inputRegisters = responseItem.getInputRegisters();

        Object fieldValue = null;

        switch (dataType) {
            case "FLOAT":
                byte[] lowBytes = inputRegisters.get(0).toBytes();
                byte[] highBytes = inputRegisters.get(1).toBytes();

                fieldValue = toFloat(lowBytes, highBytes, scale);
                break;
            case "UINT":
                if (field.getType().equals(int.class)) {
                    fieldValue = (inputRegisters.get(0).toUnsignedShort() / scale);
                } else if (field.getType().equals(float.class)) {
                    fieldValue = (inputRegisters.get(0).toUnsignedShort() / (float) scale);
                }
                break;
        }

        return fieldValue;
    }
}
