package pms.scheduler.device.airconditioner;

import org.quartz.*;
import pms.common.util.DateTimeUtil;
import pms.communication.device.airconditioner.AirConditionerClient;
import pms.communication.web.WebSender;
import pms.vo.device.AirConditionerVO;
import pms.vo.device.control.ControlRequestVO;
import pms.vo.device.control.ControlResponseVO;
import pms.vo.system.DeviceVO;
import pms.vo.system.PmsVO;

import java.util.List;

@DisallowConcurrentExecution
public class AirConditionerJob implements Job {
    private final AirConditionerClient airConditionerClient = new AirConditionerClient();
    private final WebSender webSender = new WebSender();
    private String testTime = String.valueOf(DateTimeUtil.getUnixTimestamp());

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        JobDataMap jobDataMap = jobExecutionContext.getJobDetail().getJobDataMap();
        String airConditionerCode = (String) jobDataMap.get("airConditionerCode");
        executeCommunication(airConditionerCode);
    }

    private void executeCommunication(String airConditionerCode) {
        if (airConditionerClient.isConnected(airConditionerCode)) {
            try {
                if (!airConditionerClient.isControlRequest(airConditionerCode)) {
                    executeRead(airConditionerCode);
                } else {
                    executeControl(airConditionerCode);
                }
            } finally {
                airConditionerClient.disconnect(airConditionerCode);
            }
        } else {
            try {
                airConditionerClient.connect(airConditionerCode);
            } catch (Exception e) {
                executeConnectionError(airConditionerCode);
                airConditionerClient.disconnect(airConditionerCode);
                e.printStackTrace();
            }
        }
    }

    private void executeConnectionError(String airConditionerCode) {
        AirConditionerVO airConditionerVO = airConditionerClient.readByError(airConditionerCode);
        sendReadData(airConditionerCode, airConditionerVO);
    }

    private void executeControl(String airConditionerCode) {
        ControlResponseVO responseVO = airConditionerClient.control(airConditionerCode);

        String remoteId = responseVO.getRequestVO().getRemoteId();
        String deviceCode = responseVO.getRequestVO().getDeviceCode();
        String controlCode = responseVO.getRequestVO().getControlCode();
        int result = responseVO.getResult();

        try {
            webSender.sendResponse(remoteId, deviceCode, controlCode, result, "");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void executeRead(String airConditionerCode) {
        AirConditionerVO airConditionerVO = airConditionerClient.read(airConditionerCode);

        sendReadData(airConditionerCode, airConditionerVO);
    }

    private void sendReadData(String airConditionerCode, AirConditionerVO airConditionerVO) {
        List<String> errorCodes = airConditionerClient.getErrorCodes();
        DeviceVO airConditionerInfo = airConditionerClient.getAirConditionerInfo(airConditionerCode);

        webSender.sendData(airConditionerInfo, airConditionerVO, errorCodes);
    }
}
