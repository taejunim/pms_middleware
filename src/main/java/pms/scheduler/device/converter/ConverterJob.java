package pms.scheduler.device.converter;

import org.apache.plc4x.java.api.exceptions.PlcConnectionException;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import pms.communication.device.converter.ConverterClient;
import pms.communication.web.WebSender;
import pms.vo.device.ConverterVO;
import pms.vo.device.control.ControlResponseVO;
import pms.vo.system.DeviceVO;
import pms.vo.system.PmsVO;

import java.util.List;

public class ConverterJob implements Job {
    private final ConverterClient converterClient = new ConverterClient();
    private final WebSender webSender = new WebSender();

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        executeCommunication();
    }

    private void executeCommunication() {
        if (converterClient.isConnected()) {
            try {
                if (converterClient.isControlRequest()) {
                    executeControl();
                }

                executeRead();
            } finally {
                converterClient.disconnect();
            }
        } else {
            try {
                converterClient.connect();
            } catch (PlcConnectionException e) {
                e.printStackTrace();
                converterClient.disconnect();
            }
        }
    }

    private void executeRead() {
        ConverterVO converterVO = converterClient.read();
        //System.out.println(converterVO.getDcInverters());

        System.out.println(converterVO);
        sendReadData(converterVO);
    }

    private void sendReadData(ConverterVO converterVO) {
        for (String categorySub : PmsVO.converters.keySet()) {
            DeviceVO deviceVO = PmsVO.converters.get(categorySub);
            List<String> errorCodes = null;

            webSender.sendData(deviceVO, converterVO, null);
        }
    }

    private void executeControl() {
        ControlResponseVO responseVO = converterClient.control();
        String requestType = responseVO.getRequestVO().getType();

        if (requestType.equals("02")) {
            sendControlResponse(responseVO);
        }
    }

    private void sendControlResponse(ControlResponseVO responseVO) {
        int result = responseVO.getResult();
        String remoteId = responseVO.getRequestVO().getRemoteId();
        String converterCode = responseVO.getRequestVO().getDeviceCode();
        String controlCode = responseVO.getRequestVO().getControlCode();

        webSender.sendResponse(remoteId, converterCode, controlCode, result, "");  //제어응답전송수정
    }
}
