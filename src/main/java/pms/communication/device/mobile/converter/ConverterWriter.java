package pms.communication.device.mobile.converter;

import org.apache.plc4x.java.api.PlcConnection;
import org.apache.plc4x.java.api.messages.PlcWriteRequest;
import org.apache.plc4x.java.api.messages.PlcWriteResponse;
import org.apache.plc4x.java.api.types.PlcResponseCode;
import pms.system.ess.ControlUtil;
import pms.vo.device.control.ControlRequestVO;
import pms.vo.device.control.ControlResponseVO;

import java.util.concurrent.ExecutionException;

public class ConverterWriter {
    private PlcConnection connection;
    private ControlRequestVO requestVO = new ControlRequestVO();
    //private Map<String, ControlRequestVO> controlRequestMap = new HashMap<>();
    private ControlResponseVO responseVO = new ControlResponseVO();

    public void setRequest(PlcConnection connection, ControlRequestVO requestVO) {
        this.connection = connection;
        this.requestVO = requestVO;
    }

    public void request() {
        if (connection.getMetadata().canWrite()) {
            PlcWriteRequest writeRequest = setWriteRequest();
            PlcWriteResponse writeResponse = getWriteResponse(writeRequest);

            if(writeResponse != null) {
                for (String fieldName : writeRequest.getFieldNames()) {
                    //수정 필요
                    if (writeResponse.getResponseCode(fieldName) == PlcResponseCode.OK) {
                        System.out.println(fieldName);
                        System.out.println(fieldName + " : write success.");

                        System.out.println(requestVO.getAddress() + " / " + requestVO.getControlValue() + " / " + requestVO);
                        //수정 필요
                        responseVO = ControlUtil.setControlResponseVO(requestVO.getAddress(), (short) requestVO.getControlValue(), requestVO);

                        System.out.println(responseVO);
                    } else {
                        System.out.println("[Error]Response Code : " + writeResponse.getResponseCode(fieldName));
                    }
                }
            } else {
                System.out.println("????!!!!");
            }
        }
    }

    private PlcWriteRequest setWriteRequest() {
        String address = "4x" + String.format("%04d", requestVO.getAddress());
        String itemName = requestVO.getControlCode();
        int itemValue = requestVO.getControlValue();

        PlcWriteRequest.Builder builder = connection.writeRequestBuilder();
        builder.addItem(itemName, address, itemValue);

        return builder.build();
    }

    private PlcWriteResponse getWriteResponse(PlcWriteRequest writeRequest) {
        try {
            return writeRequest.execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return null;
        }
    }

    public ControlResponseVO getResponse() {
        System.out.println("Get Response : " + responseVO);
        return responseVO;
    }
}
