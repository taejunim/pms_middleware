package pms.scheduler.device.mobile;

import  org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import pms.communication.device.mobile.ioboard.IOBoardClient;
import pms.communication.web.WebSender;
import pms.vo.device.AirConditionerVO;
import pms.vo.device.SensorVO;
import pms.vo.device.control.ControlResponseVO;
import pms.vo.device.error.DeviceErrorVO;
import pms.vo.system.DeviceVO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@DisallowConcurrentExecution
public class IOBoardJob implements Job {
    private IOBoardClient ioBoardClient = new IOBoardClient();
    private WebSender webSender = new WebSender();


    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        executeCommunication();
    }

    private void executeCommunication() {
        if (ioBoardClient.isConnected()) {
            try {
                if (!ioBoardClient.isControlRequest()) {
                    if (!ioBoardClient.getFanPowerState()) {
                        executeFanPowerOn();
                    }
                    executeRead();
                } else {
                    executeControl();
                }
            } finally {
                ioBoardClient.disconnect();
            }
        } else {
            try {
                ioBoardClient.connect();
            } catch (Exception e) {
                e.printStackTrace();
                ioBoardClient.disconnect();
                executeConnectionError();
            }
        }
    }

    private void executeRead(){
        ioBoardClient.read();
        Map<String, SensorVO> sensorsData = ioBoardClient.getSensorDataMap();
        Map<String, AirConditionerVO> airConditionersData = ioBoardClient.getAirConditionerDataMap();

        sendAllReadData(sensorsData, airConditionersData);
    }

    private void executeControl(){
        ControlResponseVO responseVO = ioBoardClient.control();

        String remoteId = responseVO.getRequestVO().getRemoteId();
        String deviceCode = responseVO.getRequestVO().getDeviceCode();
        String controlCode = responseVO.getRequestVO().getControlCode();
        int result = responseVO.getResult();

        webSender.sendResponse(remoteId, deviceCode, controlCode, result, "");
    }

    private void executeConnectionError() {
        ioBoardClient.readByError();
        Map<String, SensorVO> sensorsData = ioBoardClient.getSensorDataMap();
        Map<String, AirConditionerVO> airConditionersData = ioBoardClient.getAirConditionerDataMap();
        sendAllReadData(sensorsData, airConditionersData);
    }

    private void sendAllReadData(Map<String, SensorVO> sensorsData, Map<String, AirConditionerVO> airConditionersData) {
        Map<String, DeviceErrorVO> sensorErrorCodeMap = ioBoardClient.getSensorErrorDataMap();
        Map<String, DeviceErrorVO> airConditionerErrorCodeMap = ioBoardClient.getAirConditionerErrorDataMap();

        for (Map.Entry<String, SensorVO> entry : sensorsData.entrySet()) {
            DeviceVO sensorInfo = ioBoardClient.getSensorDeviceVosMap().get(entry.getKey());
            List<String> errCodes = new ArrayList<>();

            if (sensorErrorCodeMap.containsKey(entry.getKey())) {
                errCodes.add(sensorErrorCodeMap.get(entry.getKey()).getErrorCode());
            }
            webSender.sendData(sensorInfo, entry.getValue(), errCodes);
        }

        for (Map.Entry<String, AirConditionerVO> entry : airConditionersData.entrySet()) {
            DeviceVO airConditionerInfo = ioBoardClient.getAirConditionerDeviceVosMap().get(entry.getKey());
            List<String> errCodes = new ArrayList<>();

            if (airConditionerErrorCodeMap.containsKey(entry.getKey())) {
                errCodes.add(airConditionerErrorCodeMap.get(entry.getKey()).getErrorCode());
            }
            webSender.sendData(airConditionerInfo, entry.getValue(), errCodes);
        }
    }

    /**
     * 흡기&배기 팬 전원 ON
     */
    private void executeFanPowerOn() {
        ioBoardClient.powerOnFans();
    }
}