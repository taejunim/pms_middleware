package pms.communication.device.converter;

import org.apache.plc4x.java.api.PlcConnection;
import org.apache.plc4x.java.api.messages.PlcReadRequest;
import org.apache.plc4x.java.api.messages.PlcReadResponse;
import org.apache.plc4x.java.api.types.PlcResponseCode;
import pms.common.util.DateTimeUtil;
import pms.vo.device.ConverterVO;
import pms.vo.system.DeviceVO;
import pms.vo.system.PmsVO;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class ConverterReader {
    private PlcConnection connection;
    private List<ConverterVO.RequestItem> requestItems;
    private final ConverterVO converterVO = new ConverterVO();
    private final ConverterVO.ACConverterVO acConverterVO = new ConverterVO.ACConverterVO();
    private final Map<Integer, ConverterVO.ACInverterVO> acInverters = new HashMap<>();
    private final ConverterVO.DCConverterVO dcConverterVO = new ConverterVO.DCConverterVO();
    private final Map<Integer, ConverterVO.DCInverterVO> dcInverters = new HashMap<>();

    public void setRequest(PlcConnection connection, List<ConverterVO.RequestItem> requestItems) {
        this.connection = connection;
        this.requestItems = requestItems;
    }

    public void request() {
        PlcReadRequest readRequest = setReadRequest();
        PlcReadResponse readResponse = getReadResponse(readRequest);

        if (readResponse != null) {
            setConverterVO(readResponse);
        } else {
            //수신 오류
        }
    }

    private PlcReadRequest setReadRequest() {
        /*
         * 주소 작성 방식
         * - 3x00001 or 3x00001[1]
         * - 300001 or 300001[1]
         * - 3x00001[2], ex) 3x00001 ~ 3x00002
         * - 3x00001:INT[1], 3x00001:UINT[1], 3x00002:DINT[2]
         * - input-register:1 (Read Only)
         * - holding-register:1 (Read & Write Only)
         *
         * - input-register : 3x, Function Code : 04
         * - holding-register : 4x, Function Code: 03
         */
        PlcReadRequest.Builder builder = connection.readRequestBuilder();

        for (ConverterVO.RequestItem requestItem : requestItems) {
            String functionCode = requestItem.getFunctionCode();
            String prefix = "3x";
            String itemGroup = requestItem.getGroup();
            String itemName = requestItem.getName();
            String address = requestItem.getAddress();
            String dataType = requestItem.getDataType();
            int size = requestItem.getSize();

            if (functionCode.equals("03")) {
                prefix = "4x";
            }

            //임시 - 테스트용
            if (functionCode.equals("04")) {
                prefix = "4x";
            }

            String item = itemGroup + "_" + itemName;
            String itemAddress = prefix + address + ":" + dataType + "[" + size + "]";

            builder.addItem(item, itemAddress);
        }

        return builder.build();
    }

    private PlcReadResponse getReadResponse(PlcReadRequest readRequest) {
        try {
            return readRequest.execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return null;
        }
    }

    private int getItemValue(String item, PlcReadResponse readResponse) {
        int value = 0;

        if (readResponse.getResponseCode(item) == PlcResponseCode.OK) {
            int size = readResponse.getNumberOfValues(item);

            if (size == 1) {
                value = readResponse.getInteger(item);
            } else if (size == 2) {
                Integer lowInteger = readResponse.getInteger(item, 0);    //Low Integer
                Integer highInteger = readResponse.getInteger(item, 1) << 16; //High Integer

                value = highInteger + lowInteger;
            }
        }

        return value;
    }

    public ConverterVO getConverterVO() {
        return converterVO;
    }

    private void setConverterVO(PlcReadResponse readResponse) {
        int currentDate = DateTimeUtil.getUnixTimestamp();
        setDefaultData(currentDate);

        for (ConverterVO.RequestItem requestItem : requestItems) {
            String itemGroup = requestItem.getGroup();
            String itemName = requestItem.getName();
            String itemType = requestItem.getType();
            int scale = requestItem.getScale();

            String item = itemGroup + "_" + itemName;
            int itemValue = getItemValue(item, readResponse);

            switch (itemGroup) {
                case "ac":
                    setReadData(acConverterVO, itemGroup, itemName, itemType, scale, itemValue);
                    break;
                case "dc":
                    setReadData(dcConverterVO, itemGroup, itemName, itemType, scale, itemValue);
                    break;
                case "ac-left":
                    setReadData(acInverters.get(1), itemGroup, itemName, itemType, scale, itemValue);
                    break;
                case "ac-right":
                    setReadData(acInverters.get(2), itemGroup, itemName, itemType, scale, itemValue);
                    break;
                case "dc-left":
                    setReadData(dcInverters.get(1), itemGroup, itemName, itemType, scale, itemValue);
                    break;
                case "dc-right":
                    setReadData(dcInverters.get(2), itemGroup, itemName, itemType, scale, itemValue);
                    break;
            }
        }

        float sumPower = 0;

        for (ConverterVO.ACInverterVO acInverterVO : acInverters.values()) {
            sumPower += acInverterVO.getPower();
        }

        float averagePower = (sumPower / 2);

        acConverterVO.setTotalPower(averagePower);
        acConverterVO.setWarningFlag("N");
        acConverterVO.setFaultFlag("N");
        converterVO.setAcConverter(acConverterVO);
        converterVO.setAcInverters(new ArrayList<>(acInverters.values()));

        dcConverterVO.setWarningFlag("N");
        dcConverterVO.setFaultFlag("N");
        converterVO.setDcConverter(dcConverterVO);
        converterVO.setDcInverters(new ArrayList<>(dcInverters.values()));
    }

    private void setDefaultData(int currentDate) {
        for (String categorySub : PmsVO.converters.keySet()) {
            DeviceVO deviceVO = PmsVO.converters.get(categorySub);
            String converterCode = deviceVO.getDeviceCode();
            List<DeviceVO.ComponentVO> components = PmsVO.inverters.get(converterCode);

            switch (categorySub) {
                case "0301":    //AC/DC 컨버터
                    acConverterVO.setConverterCode(converterCode);
                    acConverterVO.setRegDate(currentDate);

                    for (DeviceVO.ComponentVO componentVO : components) {
                        int inverterNo = componentVO.getComponentNo();

                        ConverterVO.ACInverterVO acInverterVO = new ConverterVO.ACInverterVO();
                        acInverterVO.setConverterCode(converterCode);
                        acInverterVO.setRegDate(currentDate);
                        acInverterVO.setInverterNo(inverterNo);

                        acInverters.put(inverterNo, acInverterVO);
                    }
                    break;
                case "0302":    //DC/DC 컨버터
                    dcConverterVO.setConverterCode(converterCode);
                    dcConverterVO.setRegDate(currentDate);

                    for (DeviceVO.ComponentVO componentVO : components) {
                        int inverterNo = componentVO.getComponentNo();

                        ConverterVO.DCInverterVO dcInverterVO = new ConverterVO.DCInverterVO();
                        dcInverterVO.setConverterCode(converterCode);
                        dcInverterVO.setRegDate(currentDate);
                        dcInverterVO.setInverterNo(inverterNo);

                        dcInverters.put(inverterNo, dcInverterVO);
                    }
                    break;
            }
        }
    }

    private boolean isEqualItem(Class<?> voClass, String itemName) {
        for (Field field : voClass.getDeclaredFields()) {
            field.setAccessible(true);
            String fieldName = field.getName();

            if (fieldName.equals(itemName)) {
                return true;
            }
        }

        return false;
    }

    private Object setFieldValue(Class<?> fieldType, int scale, int itemValue) {
        Object fieldValue = null;

        if (fieldType.equals(int.class)) {
            fieldValue = itemValue / scale;
        } else if (fieldType.equals(float.class)) {
            fieldValue = (float) itemValue / (float) scale;
        }

        return fieldValue;
    }

    private String setStatusCode(String itemGroup, String itemName, int itemValue) {
        switch (itemName) {
            case "operationStatus":
                return getOperationStatus(itemValue);
            case "operationModeStatus":
                return getOperationModeStatus(itemValue);
            case "setOperationMode":
                return getSetOperationMode(itemValue);
            case "modeStatus":
                return getModeStatus(itemGroup, itemValue);
            case "inverterStatus":
                return getInverterStatus(itemValue);
            default:
                return null;
        }
    }

    private String getOperationStatus(int itemValue) {
        switch (itemValue) {
            case 0: //0=Boot
                return "07";  //07: 미준비
            case 1: //1=Ready
                return "08";  //08: 준비
            case 2: //2=Not Fault & Run
                return "12";  //12: 운전
            case 3: //3=Fault
                return "99";  //99: 결함
            default:
                return null;
        }
    }

    private String getOperationModeStatus(int itemValue) {
        switch (itemValue) {
            case 0: //0=N/A
                return "3";   //9: N/A
            case 1: //1=Discharge
                return "2";   //2: 방전
            case 2: //2=Charge
                return "1";   //1: 충전
            case 3: //3=Standby
                return "0";   //0: 대기
            default:
                return null;
        }
    }

    private String getSetOperationMode(int itemValue) {
        switch (itemValue) {
            case 0: //0=N/A
                return "3";   //9: N/A
            case 1: //1=Charge
                return "1";   //1: 충전
            case 2: //2=Discharge
                return "2";   //2: 방전
            default:
                return null;
        }
    }

    private String getModeStatus(String itemGroup, int itemValue) {
        switch (itemValue) {
            case 0: //0=Stop or 0=N/A
                switch (itemGroup) {
                    case "ac-left":
                    case "ac-right":
                        return "11";   //11: 정지
                    case "dc-left":
                    case "dc-right":
                        return "00";   //00: N/A
                }
            case 1: //1=Online
                return "04";   //04: ON
            case 2: //2=Offline
                return "03";   //03: OFF
            case 3: //3=Fault
                return "99";   //99: 결함
            default:
                return null;
        }
    }

    private String getInverterStatus(int itemValue) {
        switch (itemValue) {
            case 0: //0=Stop
                return "11";   //11: 정지
            case 1: //1=Run
                return "12";   //12: 운전
            case 2: //2=Ready
                return "08";   //08: 준비
            case 3: //3=Fault
                return "99";   //99: 결함
            case 4: //4=Warning
                return "98";   //98: 경고
            default:
                return null;
        }
    }

    private void setReadData(Object vo, String itemGroup, String itemName, String itemType, int scale, int itemValue) {
        boolean isEqual = isEqualItem(vo.getClass(), itemName);
        Object fieldValue = null;

        if (isEqual) {
            try {
                Field field = vo.getClass().getDeclaredField(itemName);
                field.setAccessible(true);

                if (itemType.equals("value")) {
                    fieldValue = setFieldValue(field.getType(), scale, itemValue);
                } else if (itemType.equals("status")) {
                    fieldValue = setStatusCode(itemGroup, itemName, itemValue);
                }

                field.set(vo, fieldValue);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }
}
