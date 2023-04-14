package pms.scheduler.device.bms;

import org.quartz.*;
import pms.communication.device.bms.BMSClient;
import pms.communication.web.WebSender;
import pms.system.ess.ESSController;
import pms.system.ess.ESSManager;
import pms.vo.device.BmsVO;
import pms.vo.device.control.ControlResponseVO;
import pms.vo.system.DeviceVO;
import pms.vo.system.PmsVO;

import java.sql.SQLException;
import java.util.List;

/**
 * BMS Job
 * <p>
 * - BMS 통신 스케줄러 작업 실행
 */
public class BMSJob implements Job {
    private final BMSClient bmsClient = new BMSClient();
    private final WebSender webSender = new WebSender();

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
        String rackCode = (String) jobDataMap.get("rackCode");

        executeCommunication(rackCode);
    }

    private void executeCommunication(String rackCode) {
        //System.out.println("Connected : " + bmsClient.isConnected(rackCode));
        if (bmsClient.isConnected(rackCode)) {
            try {
                if (!bmsClient.isControlRequest(rackCode)) {
                    //System.out.println("Read 1.");
                    executeRead(rackCode);
                    //System.out.println("Read 2.");
                } else {
                    executeControl(rackCode);
                }
            } finally {
                //System.out.println("Read 3.");
                bmsClient.disconnect(rackCode);
                //System.out.println("Disconnected : " + !bmsClient.isConnected(rackCode));
            }
        } else {
            try {
                //System.out.println("Read 4.");
                bmsClient.connect(rackCode);
            } catch (Exception e) {
                //System.out.println("Connected Error.");
                executeConnectionError(rackCode);
                bmsClient.disconnect(rackCode);

                for (DeviceVO rack : PmsVO.racks) {
                    if (rack.getDeviceCode().equals(rackCode)) {
                        bmsClient.setConnection(rackCode, rack);
                    }
                }

                //e.printStackTrace();
                e.getLocalizedMessage();
            }
            //System.out.println("Read 5.");
        }
    }

    private void executeRead(String rackCode) {
        BmsVO bmsVO = bmsClient.read(rackCode);
        sendReadData(rackCode, bmsVO.getRack());

        BmsVO.RackVO rackVO = bmsVO.getRack();

        ESSManager essManager = new ESSManager();
        essManager.saveRackStatus(rackVO);
        essManager.saveSoC(rackVO);
        essManager.saveLimitPower(rackVO);
    }

    private void executeControl(String rackCode) {
        ControlResponseVO responseVO = bmsClient.control(rackCode);
        String requestType = responseVO.getRequestVO().getType();

        if (requestType.equals("02")) {
            sendControlResponse(responseVO);
        }
    }

    private void executeConnectionError(String rackCode) {
        try {
            BmsVO bmsVO = bmsClient.readByError(rackCode);
            sendReadData(rackCode, bmsVO.getRack());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void sendReadData(String rackCode, BmsVO.RackVO rackVO) {
        DeviceVO rackInfo = bmsClient.getRackInfo(rackCode);
        List<String> errorCodes = bmsClient.getErrorCodes(rackCode);

        webSender.sendData(rackInfo, rackVO, errorCodes);
    }

    private void sendControlResponse(ControlResponseVO responseVO) {
        int result = responseVO.getResult();
        String remoteId = responseVO.getRequestVO().getRemoteId();

        webSender.sendResponse(remoteId, "control", result);
    }
}
