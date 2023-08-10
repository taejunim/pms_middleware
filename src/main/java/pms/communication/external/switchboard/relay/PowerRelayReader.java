package pms.communication.external.switchboard.relay;

import com.ghgande.j2mod.modbus.facade.ModbusSerialMaster;
import com.ghgande.j2mod.modbus.io.ModbusSerialTransaction;
import com.ghgande.j2mod.modbus.msg.ReadInputRegistersRequest;
import com.ghgande.j2mod.modbus.msg.ReadInputRegistersResponse;
import com.ghgande.j2mod.modbus.procimg.InputRegister;
import pms.common.util.DateTimeUtil;
import pms.system.PMSCode;
import pms.vo.device.error.DeviceErrorVO;
import pms.vo.system.DeviceVO;
import pms.vo.device.external.PowerRelayVO;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * packageName    : pms.communication.external.switchboard
 * fileName       : PowerRelayReader
 * author         : tjlim
 * date           : 2023/07/25
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2023/07/25        tjlim       최초 생성
 */
public class PowerRelayReader {
    private ModbusSerialMaster connection;
    private final int unitId;
    private final String powerRelayCode;
    private Map<String, List<PowerRelayVO.RequestItem>> requestItemsMap; //수신 요청 항목 Map
    private final PowerRelayVO powerRelayVO = new PowerRelayVO();
    private final List<DeviceErrorVO> powerRelayErrors = new ArrayList<>();

    public PowerRelayReader(DeviceVO powerRelayInfo) {
        this.unitId = 128;
        this.powerRelayCode = powerRelayInfo.getDeviceCode();
    }

    public void setRequest(ModbusSerialMaster connection, Map<String, List<PowerRelayVO.RequestItem>> requestItemsMap) {
        this.connection = connection;
        this.requestItemsMap = requestItemsMap;
    }

    /**
     * BMS 수신 요청
     * <p>
     * - 수신 요청 및 수신 데이터 처리 실행
     */
    public void request() {
        //수신할 데이터 정보를 그룹별로 수신 요청
        for (String group : requestItemsMap.keySet()) {
            System.out.println(group + " 요청");
            List<PowerRelayVO.RequestItem> requestItems = requestItemsMap.get(group); //그룹별 수신 레지스터 호출

            ReadInputRegistersRequest readRequest = setReadRequest(requestItems);  //수신 요청 정보 생성
            ReadInputRegistersResponse readResponse = getReadResponse(readRequest); //수신 데이터 호출

            if (readResponse != null) {
                InputRegister[] inputRegisters = readResponse.getRegisters();
                System.out.println(Arrays.toString(inputRegisters));
                setReadData(requestItems, inputRegisters);
            } else {
                setReadDataByError("96", "01008");  //수신 오류
            }

            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private ReadInputRegistersRequest setReadRequest(List<PowerRelayVO.RequestItem> requestItems) {
        int reference = requestItems.get(0).getRegister();
        int wordCount = 0;

        for (PowerRelayVO.RequestItem requestItem : requestItems) {
            int register = requestItem.getRegister();
            int size = requestItem.getSize();

            if (reference > register) {
                reference = register;
            }

            //Word 개수가 최대 125 이상인 경우 Request 생성 중지 - BMSReadItem.java 참고
            if (wordCount >= 125) {
                if (wordCount > 125) {
                    System.out.println("[Warning]Read items count exceeded 125. Excess items are excluded.");
                }
                break;
            }

            wordCount += size;
        }

        ReadInputRegistersRequest request = new ReadInputRegistersRequest();
        request.setUnitID(unitId);
        request.setReference(reference);
        request.setWordCount(wordCount);

        return request;
    }

    private ReadInputRegistersResponse getReadResponse(ReadInputRegistersRequest request) {
        try {
            ModbusSerialTransaction transaction = new ModbusSerialTransaction(connection.getConnection());
            transaction.setRequest(request);
            transaction.execute();

            return (ReadInputRegistersResponse) transaction.getResponse();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public PowerRelayVO getReadData() {

        return powerRelayVO;
    }

    private void setReadData(List<PowerRelayVO.RequestItem> requestItems, InputRegister[] inputRegisters) {
        powerRelayVO.setRelayCode(powerRelayCode);
        powerRelayVO.setRegDate(DateTimeUtil.getUnixTimestamp());
        powerRelayVO.setStatus(PMSCode.getDeviceStatus("10"));

        for (PowerRelayVO.RequestItem requestItem : requestItems) {
            String itemType = requestItem.getType();

            int register = requestItem.getRegister();

            if (itemType.equals("value")) {

                InputRegister highInputRegister = inputRegisters[register];
                InputRegister lowInputRegister = inputRegisters[register+1];

                setValue(requestItem, highInputRegister, lowInputRegister);
            } else if (itemType.equals("status")) {

                register = register - 131;
                InputRegister inputRegister = inputRegisters[register];

                setStatus(requestItem, inputRegister);
            }
        }
    }

    public void setReadDataByError(String statusCode, String errorCodeKey) {
        powerRelayVO.setRelayCode(powerRelayCode);
        powerRelayVO.setRegDate(DateTimeUtil.getUnixTimestamp());
        powerRelayVO.setStatus(statusCode);

        String errorCode = PMSCode.getCommonErrorCode(errorCodeKey);
        setPowerRelayErrors(errorCode);
    }

    public List<DeviceErrorVO> getPowerRelayErrors() {
        return powerRelayErrors;
    }

    private void setPowerRelayErrors(String errorCode) {
        DeviceErrorVO deviceErrorVO = new DeviceErrorVO();
        deviceErrorVO.setErrorDate(powerRelayVO.getRegDate());
        deviceErrorVO.setDeviceCode(powerRelayVO.getRelayCode());
        deviceErrorVO.setErrorCode(errorCode);

        powerRelayErrors.add(deviceErrorVO);
    }

    public byte[] getByteArray(int data) {
        byte[] convertBytes = new byte[2];
        /*convertBytes[0] = (byte)((data >> 8) & 0x000000FF);
        convertBytes[1] = (byte)(data & 0x000000FF);*/
        convertBytes[0] = (byte)((data >>> 8) & 0xFF);
        convertBytes[1] = (byte)(data & 0xFF);
        return convertBytes;
    }

    public float toFloat(byte[] highBytes, byte[] lowBytes, int scale) {
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

    private void setValue(PowerRelayVO.RequestItem requestItem, InputRegister highInputRegister, InputRegister lowInputRegister) {
        try {
            String itemName = requestItem.getName();

            for (Field field : powerRelayVO.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                String fieldName = field.getName();

                if (itemName.equals(fieldName)) {
                    Object fieldValue = getFieldValue(requestItem, highInputRegister, lowInputRegister, field);
                    field.set(powerRelayVO, fieldValue);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Object getFieldValue(PowerRelayVO.RequestItem requestItem, InputRegister highInputRegister, InputRegister lowInputRegister, Field field) {
        String dataType = requestItem.getDataType();
        int scale = requestItem.getScale();
        Object fieldValue = null;

        switch (dataType) {
            case "UINT":
                if (field.getType().equals(int.class)) {
                    //fieldValue = (inputRegister.toUnsignedShort() / scale);
                    fieldValue = toFloat(getByteArray(highInputRegister.toShort()), getByteArray(lowInputRegister.toShort()), scale);
                } else if (field.getType().equals(float.class)) {
                    //fieldValue = (inputRegister.toUnsignedShort() / (float) scale);
                    fieldValue = toFloat(getByteArray(highInputRegister.toShort()), getByteArray(lowInputRegister.toShort()), scale);
                }
                break;
            case "INT":
                if (field.getType().equals(int.class)) {
                    //fieldValue = (inputRegister.toShort() / scale);
                    fieldValue = toFloat(getByteArray(highInputRegister.toShort()), getByteArray(lowInputRegister.toShort()), scale);
                } else if (field.getType().equals(float.class)) {
                    //fieldValue = (inputRegister.toShort() / (float) scale);
                    fieldValue = toFloat(getByteArray(highInputRegister.toShort()), getByteArray(lowInputRegister.toShort()), scale);
                }
                break;
        }

        return fieldValue;
    }

    private void setStatus(PowerRelayVO.RequestItem requestItem, InputRegister inputRegister) {
        String itemName = requestItem.getName();
        String status = inputRegister.toString().equals("0") ? PMSCode.getDeviceStatus("03") : PMSCode.getDeviceStatus("04");

        switch (itemName) {
            case "overVoltageRelayAction":
                powerRelayVO.setOverVoltageRelayAction(status);
                break;
            case "underVoltageRelayAction":
                powerRelayVO.setUnderVoltageRelayAction(status);
                break;
            case "overFrequencyRelayAction":
                powerRelayVO.setOverFrequencyRelayAction(status);
                break;
            case "underFrequencyRelayAction":
                powerRelayVO.setUnderFrequencyRelayAction(status);
                break;
            case "reversePowerRelayAction":
                powerRelayVO.setReversePowerRelayAction(status);
                break;
        }
    }

    private String getOnOffFlagCode(String status) {
        String statusCode = null;

        if (status.equals("1")) {
            //statusCode = PMSCode.getDeviceStatus("03");
            System.out.println("03");
        } else if (status.equals("2")) {
            //statusCode = PMSCode.getDeviceStatus("04");
            System.out.println("04");
        }

        return statusCode;
    }

    private String getOccurFlagCode(String status) {
        String statusCode = null;

        if (status.equals("0")) {
            //statusCode = PMSCode.getCommonCode("OCCUR_FLAG_N");
            System.out.println("OCCUR_FLAG_N");
        } else if (status.equals("1")) {
            //statusCode = PMSCode.getCommonCode("OCCUR_FLAG_Y");
            System.out.println("OCCUR_FLAG_Y");
        }

        return statusCode;
    }

    /*private void setPCSFault(String itemName, String status) {
        String[] bits = PCSReadItem.toBits(Integer.parseInt(status));
        String faultType = null;

        if (itemName.equals("fault1")) {
            faultType = "F1B";
        } else if (itemName.equals("fault2")) {
            faultType = "F2B";
        }

        for (int i = 15; i >= 0; i--) {
            String errorBit = bits[i];
            int errorValue = Integer.parseInt(errorBit, 2);

            if (errorValue == 1) {
                String faultCode = faultType + String.format("%02d", 15 - i);
                String errorCode = PMSCode.getPCSErrorCode(faultCode);

                setPcsErrors(errorCode);
            }
        }
    }*/
}
