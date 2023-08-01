package pms.communication.external.switchboard;

import com.ghgande.j2mod.modbus.facade.ModbusSerialMaster;
import com.ghgande.j2mod.modbus.io.ModbusSerialTransaction;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersRequest;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersResponse;
import com.ghgande.j2mod.modbus.procimg.InputRegister;
import pms.common.util.DateTimeUtil;
import pms.system.PMSCode;
import pms.vo.device.error.DeviceErrorVO;
import pms.vo.system.DeviceVO;
import pms.vo.system.PowerMeterVO;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
/**
 * packageName    : pms.communication.external.switchboard
 * fileName       : PowerMeterReader
 * author         : youyeong
 * date           : 2023/07/28
 * description    : EV충전기 전력 계측기 통신 Reader
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2023/07/28        youyeong       최초 생성
 */
public class PowerMeterReader {

    private ModbusSerialMaster connection;
    private final int unitId;
    private final String powerMeterCode;
    private Map<String, List<PowerMeterVO.RequestItem>> requestItemsMap; //수신 요청 항목 Map
    private final PowerMeterVO powerMeterVO = new PowerMeterVO();
    private final List<DeviceErrorVO> powerMeterErrors = new ArrayList<>();
    public PowerMeterReader(DeviceVO powerMeterInfo) {
        this.unitId = 1;
        this.powerMeterCode = powerMeterInfo.getDeviceCode();
    }

    public void setRequest(ModbusSerialMaster connection, Map<String, List<PowerMeterVO.RequestItem>> requestItemsMap) {
        this.connection = connection;
        this.requestItemsMap = requestItemsMap;
    }

    /**
     * 수신 요청
     * <p>
     * - 수신 요청 및 수신 데이터 처리 실행
     */
    public void request() {
        //수신할 데이터 정보를 그룹별로 수신 요청
        for (String group : requestItemsMap.keySet()) {
            List<PowerMeterVO.RequestItem> requestItems = requestItemsMap.get(group);   //그룹별 수신 레지스터 호출
            ReadMultipleRegistersRequest readRequest    = setReadRequest(requestItems); //수신 요청 정보 생성
            ReadMultipleRegistersResponse readResponse  = getReadResponse(readRequest); //수신 데이터 호출

            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (readResponse != null) {
                InputRegister[] inputRegisters = readResponse.getRegisters();
                setReadData(requestItems, inputRegisters);
            } else {
                setReadDataByError("96", "01008");  //수신 오류
            }
        }
    }

    private ReadMultipleRegistersRequest setReadRequest(List<PowerMeterVO.RequestItem> requestItems) {
        int firstReference = requestItems.get(0).getRegister();
        int lastReference  = requestItems.get(requestItems.size() - 1).getRegister();
        int lastDateSize  = requestItems.get(requestItems.size() - 1).getSize();
        int wordCount = lastReference - firstReference + lastDateSize;

        ReadMultipleRegistersRequest request = new ReadMultipleRegistersRequest();
        request.setUnitID(unitId);
        request.setReference(firstReference);
        request.setWordCount(wordCount);

        return request;
    }

    private ReadMultipleRegistersResponse getReadResponse(ReadMultipleRegistersRequest request) {
        try {
            ModbusSerialTransaction transaction = new ModbusSerialTransaction(connection.getConnection());
            transaction.setRequest(request);
            transaction.execute();

            return (ReadMultipleRegistersResponse) transaction.getResponse();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public PowerMeterVO getReadData() {
        return powerMeterVO;
    }

    private void setReadData(List<PowerMeterVO.RequestItem> requestItems, InputRegister[] inputRegisters) {
        powerMeterVO.setMeterCode(powerMeterCode);
        powerMeterVO.setRegDate(DateTimeUtil.getUnixTimestamp());
        powerMeterVO.setStatus(PMSCode.getDeviceStatus("10"));
        try {
            int firstRegisterIndex = requestItems.get(0).getRegister();
            for (PowerMeterVO.RequestItem requestItem : requestItems) {
                int registerIndex = requestItem.getRegister() - firstRegisterIndex;
                InputRegister[] registerData = Arrays.copyOfRange(inputRegisters, registerIndex, registerIndex + requestItem.getSize());
                setValue(requestItem, registerData);
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setReadDataByError(String statusCode, String errorCodeKey) {
        powerMeterVO.setMeterCode(powerMeterCode);
        powerMeterVO.setRegDate(DateTimeUtil.getUnixTimestamp());
        powerMeterVO.setStatus(statusCode);

        String errorCode = PMSCode.getCommonErrorCode(errorCodeKey);
        setPowerMeterErrors(errorCode);
    }

    public List<DeviceErrorVO> getPowerMeterErrors() {
        return powerMeterErrors;
    }

    private void setPowerMeterErrors(String errorCode) {
        DeviceErrorVO deviceErrorVO = new DeviceErrorVO();
        deviceErrorVO.setErrorDate(powerMeterVO.getRegDate());
        deviceErrorVO.setDeviceCode(powerMeterVO.getMeterCode());
        deviceErrorVO.setErrorCode(errorCode);

        powerMeterErrors.add(deviceErrorVO);
    }

    //Float32 변환
    public float toFloat(InputRegister[] inputRegisters, int scale) {

        int highInt = inputRegisters[0].toUnsignedShort();
        int lowInt  = inputRegisters[1].toUnsignedShort();

        float result = 0;
        if(highInt != 65472 || lowInt != 0) {
            int intResult = lowInt + (highInt << 16);
            result = Float.intBitsToFloat(intResult);
        }
        result = result / (float) scale;

        return result;
    }

    //Int64 변환
    public long toInt64(InputRegister[] inputRegisters, int scale) {
        byte[] bytes = {inputRegisters[0].toBytes()[0], inputRegisters[0].toBytes()[1], inputRegisters[1].toBytes()[0], inputRegisters[1].toBytes()[1], inputRegisters[2].toBytes()[0], inputRegisters[2].toBytes()[1], inputRegisters[3].toBytes()[0], inputRegisters[3].toBytes()[1]};
        ByteBuffer byte_buf = ByteBuffer.wrap(bytes);

        long result = byte_buf.getLong();

        if(result > -9223372036854775808L) {
            result = byte_buf.getLong() / (long) scale;
        } else result = 0;

        return result;
    }

    private void setValue(PowerMeterVO.RequestItem requestItem, InputRegister[] inputRegister) {
        try {
            String itemName = requestItem.getName();
            for (Field field : powerMeterVO.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                String fieldName = field.getName();

                if (itemName.equals(fieldName)) {
                    Object fieldValue = getFieldValue(requestItem, inputRegister);
                    field.set(powerMeterVO, fieldValue);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Object getFieldValue(PowerMeterVO.RequestItem requestItem, InputRegister[] inputRegisters) {
        String dataType = requestItem.getDataType();
        int scale = requestItem.getScale();
        Object fieldValue = null;

        switch (dataType) {
            case "FLOAT32":
                fieldValue = toFloat(inputRegisters, scale);
                break;
            case "INT64":
                fieldValue = toInt64(inputRegisters, scale);
                break;
        }

        return fieldValue;
    }

}
