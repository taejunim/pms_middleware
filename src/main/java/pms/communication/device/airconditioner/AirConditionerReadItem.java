package pms.communication.device.airconditioner;

import pms.vo.device.AirConditionerVO;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AirConditionerReadItem {
    private final Map<String, List<AirConditionerVO.RequestItem>> requestItemsMap = new HashMap<>();

    public AirConditionerReadItem() {
        setRequestItems();
    }

    public static Object getFieldValue(AirConditionerVO.ResponseItem responseItem, Field field) {
        String dataType = responseItem.getDataType();
        int scale = responseItem.getScale();

        Object fieldValue = null;

        switch (responseItem.getFunctionType()) {
//            case "coils":
            case "holdingRegisters":
                if (dataType.equals("UINT")) {
                    if (field.getType().equals(int.class)) {
                        fieldValue = responseItem.getMultipleRegister().toUnsignedShort() / scale;
                    } else if (field.getType().equals(float.class)) {
                        fieldValue = responseItem.getMultipleRegister().toUnsignedShort() / (float) scale;
                    }
                } else if (dataType.equals("INT")) {
                    if (field.getType().equals(int.class)) {
                        fieldValue = responseItem.getMultipleRegister().toShort() / scale;
                    } else if (field.getType().equals(float.class)) {
                        fieldValue = responseItem.getMultipleRegister().toShort() / (float) scale;
                    }
                }
                break;
            case "inputRegisters":
                if (dataType.equals("UINT")) {
                    if (field.getType().equals(int.class)) {
                        fieldValue = responseItem.getInputRegister().toUnsignedShort() / scale;
                    } else if (field.getType().equals(float.class)) {
                        fieldValue = responseItem.getInputRegister().toUnsignedShort() / (float) scale;
                    }
                } else if (dataType.equals("INT")) {
                    if (field.getType().equals(int.class)) {
                        fieldValue = responseItem.getInputRegister().toShort() / scale;
                    } else if (field.getType().equals(float.class)) {
                        fieldValue = responseItem.getInputRegister().toShort() / (float) scale;
                    }
                }
                break;
        }

        return fieldValue;
    }

    public Map<String, List<AirConditionerVO.RequestItem>> getRequestItemsMap() {
        return requestItemsMap;
    }

    private void setRequestItems() {

        //레지스터 삽입 순서(정렬) 중요*
        List<AirConditionerVO.RequestItem> requestItems1 = new ArrayList<>();
        requestItems1.add(setRequestItem("coils", "0x0000", 0, "UINT", 1, "status", "operationStatus"));
        requestItemsMap.put("airConditioner-items-coils", requestItems1);   //읽기&쓰기 - 불리언 (F:01,05)

        List<AirConditionerVO.RequestItem> requestItems2 = new ArrayList<>();
        requestItems2.add(setRequestItem("holdingRegisters", "0x0000", 0, "UINT", 1, "status", "operationModeStatus"));
        requestItems2.add(setRequestItem("holdingRegisters", "0x0001", 1, "UINT", 10, "value", "setTemperature"));
        requestItemsMap.put("airConditioner-items-holdingRegisters", requestItems2);    //읽기&쓰기 - 부호없는 워드 (F:03,06)

        List<AirConditionerVO.RequestItem> requestItems3 = new ArrayList<>();
        requestItems3.add(setRequestItem("inputRegisters", "0x0002", 2, "UINT", 10, "value", "indoorTemperature"));
        requestItems3.add(setRequestItem("inputRegisters", "0x0063", 99, "UINT", 1, "status", "errorCode"));
        requestItemsMap.put("airConditioner-items-inputRegisters", requestItems3);      //읽기만 - 부호없는 워드 (F:04)
    }

    private AirConditionerVO.RequestItem setRequestItem(String functionType, String address, int register, String dataType, int scale, String type, String name) {
        AirConditionerVO.RequestItem requestItem = new AirConditionerVO.RequestItem();

        requestItem.setFunctionType(functionType);
        requestItem.setAddress(address);
        requestItem.setRegister(register);
        requestItem.setDataType(dataType);
        requestItem.setScale(scale);
        requestItem.setType(type);
        requestItem.setName(name);

        return requestItem;
    }
}
