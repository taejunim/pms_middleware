package pms.communication.device.airconditioner;

import com.ghgande.j2mod.modbus.facade.ModbusSerialMaster;
import com.ghgande.j2mod.modbus.io.ModbusSerialTransaction;
import com.ghgande.j2mod.modbus.msg.*;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;
import pms.system.ess.ControlUtil;
import pms.vo.device.control.ControlRequestVO;
import pms.vo.device.control.ControlResponseVO;
import pms.vo.system.DeviceVO;

public class AirConditionerWriter {
    private int unitId;
    private ModbusSerialMaster connection;
    private int result;
    private String airConditionerCode;
    private ControlResponseVO responseVO = new ControlResponseVO();
    private ControlRequestVO requestVO = new ControlRequestVO();
    private String functionCode;

    public void setRequest(String airConditionerCode, ModbusSerialMaster connection, ControlRequestVO requestVO) {
        this.unitId = 1;    //!!! 고정형 에어컨 유닛아이디 확인
        this.airConditionerCode = airConditionerCode;
        this.connection = connection;
        this.requestVO = requestVO;
        this.functionCode = getFunctionCode(requestVO.getControlType());
    }

    private String getFunctionCode(String controlType) {
        String functionCode;
        if (controlType.equals("8000") || controlType.equals("8001")) {
            functionCode = "05";
        } else {
            functionCode = "06";
        }
        return functionCode;
    }

    public void request() {
        ModbusRequest writeRequest = setWriteRequest();
        ModbusResponse writeResponse = getWriteResponse(writeRequest);

        if (writeResponse != null) {
            if (functionCode.equals("05")) {
                WriteCoilResponse writeCoilResponse = (WriteCoilResponse) writeResponse;
                int address = writeCoilResponse.getReference();
                short value = 0;
                if (writeCoilResponse.getCoil()) {
                    value = 1;
                }
                responseVO = ControlUtil.setControlResponseVO(address, value, requestVO);
            } else if (functionCode.equals("06")) {
                WriteSingleRegisterResponse writeSingleRegisterResponse = (WriteSingleRegisterResponse) writeResponse;
                int address = writeSingleRegisterResponse.getReference();
                short value = (short) writeSingleRegisterResponse.getRegisterValue();
                responseVO = ControlUtil.setControlResponseVO(address, value, requestVO);
            }
            result = 1; //성공
        } else {
            System.out.println("[Air] Write Error");
            result = 0; //실패
        }
    }

    public int getResult() {
        return result;
    }

    private ModbusResponse getWriteResponse(ModbusRequest writeRequest) {
        ModbusResponse response = null;
        try {
            ModbusSerialTransaction transaction = new ModbusSerialTransaction(connection.getConnection());
            transaction.setRequest(writeRequest);
            transaction.execute();
            response = transaction.getResponse();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }

    private ModbusRequest setWriteRequest() {
        ModbusRequest request = null;
        int address = requestVO.getAddress();
        int controlValue = requestVO.getControlValue();

        if (functionCode.equals("05")) {
            WriteCoilRequest writeCoilRequest = new WriteCoilRequest();
            writeCoilRequest.setUnitID(unitId);
            writeCoilRequest.setReference(address);
            if (controlValue == 0) {
                writeCoilRequest.setCoil(false);
            } else if (controlValue == 1) {
                writeCoilRequest.setCoil(true);
            }
            request = writeCoilRequest;
        } else if (functionCode.equals("06")) {
            WriteSingleRegisterRequest writeSingleRegisterRequest = new WriteSingleRegisterRequest();
            writeSingleRegisterRequest.setUnitID(unitId);
            writeSingleRegisterRequest.setReference(address);
            writeSingleRegisterRequest.setRegister(new SimpleRegister(controlValue));
            request = writeSingleRegisterRequest;
        } else {
            System.out.println("[Air] Write FunctionCode Error!");
        }

        return request;
    }

    public ControlResponseVO getResponse() {
        return responseVO;
    }
}
