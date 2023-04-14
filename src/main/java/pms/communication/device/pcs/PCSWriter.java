package pms.communication.device.pcs;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.facade.ModbusSerialMaster;
import com.ghgande.j2mod.modbus.io.ModbusSerialTransaction;
import com.ghgande.j2mod.modbus.msg.WriteSingleRegisterRequest;
import com.ghgande.j2mod.modbus.msg.WriteSingleRegisterResponse;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;
import pms.system.ess.ControlUtil;
import pms.vo.device.control.ControlRequestVO;
import pms.vo.device.control.ControlResponseVO;

public class PCSWriter {
    private int unitId;
    private ModbusSerialMaster connection;
    private int result;
    private ControlResponseVO responseVO = new ControlResponseVO();

    public void setRequest(int unitId, ModbusSerialMaster connection) {
        this.unitId = unitId;
        this.connection = connection;
    }

    public void request(int address, int value) {
        WriteSingleRegisterRequest writeRequest = setWriteRequest(address, value);
        WriteSingleRegisterResponse writeResponse = getWriteResponse(writeRequest);

        if (writeResponse != null) {
            result = setResult(address, value, writeResponse);
        }
    }

    public int getResult() {
        return result;
    }

    private int setResult(int requestAddress, int requestValue, WriteSingleRegisterResponse response) {
        int address = response.getReference();
        int registerValue = response.getRegisterValue();

        if (requestAddress == address) {
            if (registerValue == requestValue) {
                return 1;   //1: 성공
            }
        }

        return 0;   //0: 실패
    }

    public void request(ControlRequestVO requestVO) {
        int requestAddress = requestVO.getAddress();
        int controlValue = requestVO.getControlValue();

        WriteSingleRegisterRequest writeRequest = setWriteRequest(requestAddress, controlValue);
        WriteSingleRegisterResponse writeResponse = getWriteResponse(writeRequest);

        if (writeResponse != null) {
            int address = writeResponse.getReference();
            short value = (short) writeResponse.getRegisterValue();

            responseVO = ControlUtil.setControlResponseVO(address, value, requestVO);
        }
    }

    public ControlResponseVO getResponse() {
        return responseVO;
    }

    private WriteSingleRegisterRequest setWriteRequest(int address, int value) {
        SimpleRegister register = new SimpleRegister(value);

        WriteSingleRegisterRequest request = new WriteSingleRegisterRequest();
        request.setReference(address);
        request.setRegister(register);
        request.setUnitID(unitId);

        return request;
    }

    private WriteSingleRegisterResponse getWriteResponse(WriteSingleRegisterRequest request) {
        try {
            ModbusSerialTransaction transaction = new ModbusSerialTransaction(connection.getConnection());
            transaction.setRequest(request);
            transaction.execute();

            return (WriteSingleRegisterResponse) transaction.getResponse();
        } catch (ModbusException e) {
            //e.printStackTrace();
            e.getLocalizedMessage();

            return null;
        }
    }
}
