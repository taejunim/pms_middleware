package pms.communication.device.pcs;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.facade.ModbusSerialMaster;
import com.ghgande.j2mod.modbus.io.ModbusSerialTransaction;
import com.ghgande.j2mod.modbus.msg.ReadInputRegistersRequest;
import com.ghgande.j2mod.modbus.msg.ReadInputRegistersResponse;
import com.ghgande.j2mod.modbus.procimg.InputRegister;
import pms.common.util.DateTimeUtil;
import pms.system.PMSCode;
import pms.vo.device.PcsVO;
import pms.vo.device.error.DeviceErrorVO;
import pms.vo.system.DeviceVO;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class PCSReader {
    private ModbusSerialMaster connection;
    private final int unitId;
    private final String pcsCode;
    private List<PcsVO.RequestItem> requestItems;
    private final PcsVO pcsVO = new PcsVO();
    private final List<DeviceErrorVO> pcsErrors = new ArrayList<>();
    private DeviceErrorVO commonError = new DeviceErrorVO();

    public PCSReader(DeviceVO pcsInfo) {
        this.unitId = pcsInfo.getDeviceNo();
        this.pcsCode = pcsInfo.getDeviceCode();
    }

    public void setRequest(ModbusSerialMaster connection, List<PcsVO.RequestItem> requestItems) {
        this.connection = connection;
        this.requestItems = requestItems;
    }

    public void request() {
        ReadInputRegistersRequest readRequest = setReadRequest();
        ReadInputRegistersResponse readResponse = getReadResponse(readRequest);

        if (readResponse != null) {
            InputRegister[] inputRegisters = readResponse.getRegisters();
            setReadData(inputRegisters);
        } else {
            setReadDataByError("96", "01008");  //수신 오류
        }
    }

    private ReadInputRegistersRequest setReadRequest() {
        int reference = 0;
        int wordCount = requestItems.size();

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
        }

        return null;
    }

    public PcsVO getReadData() {
        //PCS 운전 상태인 경우, 미세 전류로 인해 운전 모드가 충전 상태로 오는 문제로 임시 예외 처리
        if (pcsVO.getOperationStatus().equals("12")) {
            String operationMode = pcsVO.getOperationModeStatus();
            float outputPower = pcsVO.getOutputPower();

            boolean isChargeMode = (operationMode.equals("1") && outputPower <= 2.0);   //충전 모드 판단(영점 조절)
            boolean isDischargeMode = (operationMode.equals("2") && outputPower >= -2.0);   //방전 모드 판단(영점 조절)

            if (isChargeMode || isDischargeMode) {
                pcsVO.setOperationModeStatus("0");
            }

            /*if ((pcsVO.getOutputPower() <= 2.0 && pcsVO.getOperationModeStatus().equals("1"))) {
                pcsVO.setOperationModeStatus("0");
            }*/
        }

        if (pcsVO.getWarningFlag() == null) {
            pcsVO.setWarningFlag("N");
        }

        return pcsVO;
    }

    private void setReadData(InputRegister[] inputRegisters) {
        pcsVO.setPcsCode(pcsCode);
        pcsVO.setRegDate(DateTimeUtil.getUnixTimestamp());

        for (PcsVO.RequestItem requestItem : requestItems) {
            String itemType = requestItem.getType();
            int register = requestItem.getRegister();
            InputRegister inputRegister = inputRegisters[register];

            if (itemType.equals("value")) {
                setValue(requestItem, inputRegister);
            } else if (itemType.equals("status")) {
                setStatus(requestItem, inputRegister);
            }
        }
    }

    public void setReadDataByError(String statusCode, String errorCodeKey) {
        pcsVO.setPcsCode(pcsCode);
        pcsVO.setRegDate(DateTimeUtil.getUnixTimestamp());
        pcsVO.setOperationStatus(statusCode);
        pcsVO.setOperationModeStatus("0");
        pcsVO.setWarningFlag("Y");
        pcsVO.setFaultFlag("N");

        String errorCode = PMSCode.getCommonErrorCode(errorCodeKey);
        //setPcsErrors(errorCode);
        commonError = setErrorVO(errorCode);
    }

    public DeviceErrorVO getCommonError() {
        return commonError;
    }

    public List<DeviceErrorVO> getPcsErrors() {
        return pcsErrors;
    }

    private DeviceErrorVO setErrorVO(String errorCode) {
        DeviceErrorVO deviceErrorVO = new DeviceErrorVO();
        deviceErrorVO.setErrorDate(pcsVO.getRegDate());
        deviceErrorVO.setDeviceCode(pcsVO.getPcsCode());
        deviceErrorVO.setErrorCode(errorCode);

        return deviceErrorVO;
    }

    private void setPcsErrors(String errorCode) {
        DeviceErrorVO deviceErrorVO = new DeviceErrorVO();
        deviceErrorVO.setErrorDate(pcsVO.getRegDate());
        deviceErrorVO.setDeviceCode(pcsVO.getPcsCode());
        deviceErrorVO.setErrorCode(errorCode);

        pcsErrors.add(deviceErrorVO);
    }

    private void setValue(PcsVO.RequestItem requestItem, InputRegister inputRegister) {
        try {
            String itemName = requestItem.getName();

            for (Field field : pcsVO.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                String fieldName = field.getName();

                if (itemName.equals(fieldName)) {
                    Object fieldValue = getFieldValue(requestItem, inputRegister, field);
                    field.set(pcsVO, fieldValue);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Object getFieldValue(PcsVO.RequestItem requestItem, InputRegister inputRegister, Field field) {
        String dataType = requestItem.getDataType();
        int scale = requestItem.getScale();
        Object fieldValue = null;

        switch (dataType) {
            case "UINT":
                if (field.getType().equals(int.class)) {
                    fieldValue = (inputRegister.toUnsignedShort() / scale);
                } else if (field.getType().equals(float.class)) {
                    fieldValue = (inputRegister.toUnsignedShort() / (float) scale);
                }
                break;
            case "INT":
                if (field.getType().equals(int.class)) {
                    fieldValue = (inputRegister.toShort() / scale);
                } else if (field.getType().equals(float.class)) {
                    fieldValue = (inputRegister.toShort() / (float) scale);
                }
                break;
        }

        return fieldValue;
    }

    private void setStatus(PcsVO.RequestItem requestItem, InputRegister inputRegister) {
        String itemName = requestItem.getName();
        String status = inputRegister.toString();

        switch (itemName) {
            case "operationReadyStatus":
                if (status.equals("0")) {
                    pcsVO.setOperationStatus(PMSCode.getDeviceStatus("07"));
                }
                break;
            case "operationStatus":
                if (status.equals("0")) {
                    pcsVO.setOperationStatus(PMSCode.getDeviceStatus("11"));
                } else if (status.equals("1")) {
                    pcsVO.setOperationStatus(PMSCode.getDeviceStatus("12"));
                }
                break;
            case "operationModeStatus":
                pcsVO.setOperationModeStatus(PMSCode.getOperationMode(status));
                break;
            case "acMainMcStatus":
                pcsVO.setAcMainMcStatus(getOnOffFlagCode(status));
                break;
            case "dcMainMcStatus":
                pcsVO.setDcMainMcStatus(getOnOffFlagCode(status));
                break;
            case "emergencyStopFlag":
                pcsVO.setEmergencyStopFlag(getOccurFlagCode(status));

                if (pcsVO.getEmergencyStopFlag().equals("Y")) {
                    pcsVO.setOperationStatus(PMSCode.getDeviceStatus("13"));
                }
                break;
            case "faultFlag":
                pcsVO.setFaultFlag(getOccurFlagCode(status));
                break;
            case "fault1":
            case "fault2":
                setPCSFault(itemName, status);
                break;
        }
    }

    private String getOnOffFlagCode(String status) {
        String statusCode = null;

        if (status.equals("1")) {
            statusCode = PMSCode.getDeviceStatus("03");
        } else if (status.equals("2")) {
            statusCode = PMSCode.getDeviceStatus("04");
        }

        return statusCode;
    }

    private String getOccurFlagCode(String status) {
        String statusCode = null;

        if (status.equals("0")) {
            statusCode = PMSCode.getCommonCode("OCCUR_FLAG_N");
        } else if (status.equals("1")) {
            statusCode = PMSCode.getCommonCode("OCCUR_FLAG_Y");
        }

        return statusCode;
    }

    private void setPCSFault(String itemName, String status) {
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

                //setPcsErrors(errorCode);

                DeviceErrorVO errorVO = setErrorVO(errorCode);
                pcsErrors.add(errorVO);
            }
        }
    }
}
