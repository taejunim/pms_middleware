package pms.scheduler.device.pcs;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import pms.communication.device.pcs.PCSClient;
import pms.communication.external.smarthub.EVChargerClientNew;
import pms.communication.web.WebSender;
import pms.system.ess.ESSController;
import pms.system.ess.ESSManager;
import pms.vo.device.PcsVO;
import pms.vo.device.control.ControlRequestVO;
import pms.vo.device.control.ControlResponseVO;
import pms.vo.system.DeviceVO;

import java.util.List;

public class PCSJob implements Job {
    private final PCSClient pcsClient = new PCSClient();
    private final WebSender webSender = new WebSender();

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        executeCommunication();
    }

    private void executeCommunication() {
        if (pcsClient.isConnected()) {
            try {
                if (!pcsClient.isControlRequest()) {
                    if (!pcsClient.checkHeartbeatInterval()) {
                        executeRead();
                    } else {
                        sendHeartbeat();
                    }
                } else {
                    executeControl();
                }
            } finally {
                pcsClient.disconnect();
            }
        } else {
            try {
                pcsClient.connect();
            } catch (Exception e) {
                executeConnectionError();
                pcsClient.disconnect();

                e.printStackTrace();
            }
        }
    }

    private void executeRead() {
        PcsVO pcsVO = pcsClient.read();

        ESSManager essManager = new ESSManager();
        essManager.processEnergyData(pcsVO);

        sendReadData(pcsVO);

        ESSController essController = new ESSController();
        essController.autoControlPCS(pcsVO);
    }

    private void executeConnectionError() {
        PcsVO pcsVO = pcsClient.readByError();
        sendReadData(pcsVO);
    }

    private void sendHeartbeat() {
        int result = pcsClient.sendHeartbeat();

        if (result == 1) {
            pcsClient.resetHeartbeatInterval();
        }
    }

    private void executeControl() {
        ControlResponseVO responseVO = pcsClient.control();
        ControlRequestVO requestVO = responseVO.getRequestVO();

        System.out.println("제어 실행 : " + requestVO);

        String requestType = requestVO.getType();
        String requestDetailType = requestVO.getDetailType();
        String controlCode = requestVO.getControlCode();

        if (controlCode.equals("0200010205") || controlCode.equals("0200010206")) {
            new ESSManager().setOperationType(requestType);
        }

        if (requestType.equals("02")) {
            sendControlResponse(responseVO);
        } else if (requestType.equals("05")) {
            System.out.println("[EV 충전기] 제어 요청");

            if (!requestDetailType.equals("0500")) {
                if (responseVO.getResult() == 1) {
                    System.out.println("[EV 충전기] 제어 요청 완료, 요청 제거");
                    //new EVChargerClient().removeEVChargerRequest();
                    new EVChargerClientNew().resetControlRequest();
                }
            } else {
                System.out.println("[EV 충전기] PCS 운전 제어 요청");
            }

            System.out.println(requestType + "-Result(05) : " + responseVO.getResult());
        } else {
            System.out.println(requestType + "-Result(Not 02, 05) : " + responseVO.getResult());
        }
    }

    private void sendReadData(PcsVO pcsVO) {
        List<String> errorCodes = pcsClient.getErrorCodes();
        DeviceVO pcsInfo = pcsClient.getPcsInfo();

        webSender.sendData(pcsInfo, pcsVO, errorCodes);
    }

    private void sendControlResponse(ControlResponseVO responseVO) {
        int result = responseVO.getResult();
        String remoteId = responseVO.getRequestVO().getRemoteId();

        webSender.sendResponse(remoteId, "control", result);
    }
}
