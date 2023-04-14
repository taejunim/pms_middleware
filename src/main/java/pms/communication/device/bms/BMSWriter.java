package pms.communication.device.bms;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.io.ModbusTCPTransaction;
import com.ghgande.j2mod.modbus.msg.WriteSingleRegisterRequest;
import com.ghgande.j2mod.modbus.msg.WriteSingleRegisterResponse;
import com.ghgande.j2mod.modbus.net.TCPMasterConnection;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;
import pms.system.ess.ControlUtil;
import pms.vo.device.control.ControlRequestVO;
import pms.vo.device.control.ControlResponseVO;

/**
 * BMS Writer
 * <p>
 * - BMS 데이터 송신
 */
public class BMSWriter {
    private TCPMasterConnection connection;   //통신 연결 정보
    private String rackCode;  //Rack 코드
    private int rackNo;   //Rack 번호
    private ControlRequestVO requestVO = new ControlRequestVO();
    private ControlResponseVO responseVO = new ControlResponseVO();

    public void setRequest(String rackCode, int rackNo, TCPMasterConnection connection, ControlRequestVO requestVO) {
        this.rackCode = rackCode;
        this.rackNo = rackNo;
        this.connection = connection;
        this.requestVO = requestVO;
    }

    public void request() {
        WriteSingleRegisterRequest writeRequest = setWriteRequest();
        WriteSingleRegisterResponse writeResponse = getWriteResponse(writeRequest);

        if (writeResponse != null) {
            int address = writeResponse.getReference();
            short value = (short) writeResponse.getRegisterValue();
            responseVO = ControlUtil.setControlResponseVO(address, value, requestVO);
        } else {
            //송신 오류
        }
    }

    public ControlResponseVO getResponse() {
        return responseVO;
    }

    private WriteSingleRegisterRequest setWriteRequest() {
        int address = requestVO.getAddress();
        int controlValue = requestVO.getControlValue();
        SimpleRegister register = new SimpleRegister(controlValue);

        WriteSingleRegisterRequest writeRequest = new WriteSingleRegisterRequest();
        writeRequest.setReference(address);
        writeRequest.setRegister(register);
        writeRequest.setUnitID(rackNo);

        return writeRequest;
    }

    private WriteSingleRegisterResponse getWriteResponse(WriteSingleRegisterRequest request) {
        try {
            ModbusTCPTransaction transaction = new ModbusTCPTransaction(connection);
            transaction.setRequest(request);
            transaction.execute();

            return (WriteSingleRegisterResponse) transaction.getResponse();
        } catch (ModbusException e) {
            e.printStackTrace();
            return null;
        }
    }
}
