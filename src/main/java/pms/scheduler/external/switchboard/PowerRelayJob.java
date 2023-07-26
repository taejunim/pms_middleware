package pms.scheduler.external.switchboard;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import pms.communication.external.switchboard.PowerRelayClient;
import pms.vo.device.PcsVO;
import pms.vo.system.PowerRelayVO;

/**
 * packageName    : pms.communication.external.switchboard
 * fileName       : PowerRelayJob
 * author         : tjlim
 * date           : 2023/07/25
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2023/07/25        tjlim       최초 생성
 */
public class PowerRelayJob implements Job {
    private final PowerRelayClient powerRelayClient = new PowerRelayClient();

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        executeCommunication();
    }

    private void executeCommunication() {
        if (powerRelayClient.isConnected()) {
            try {
                System.out.println(" [ Read 시작 ] ");
                executeRead();
            } finally {
                powerRelayClient.disconnect();
            }
        } else {
            try {
                powerRelayClient.connect();
            } catch (Exception e) {
                executeConnectionError();
                powerRelayClient.disconnect();

                e.printStackTrace();
            }
        }
    }

    private void executeRead() {
        PowerRelayVO powerRelayVO = powerRelayClient.read();

        sendReadData(powerRelayVO);
    }

    private void executeConnectionError() {
        PowerRelayVO powerRelayVO = powerRelayClient.readByError();
        sendReadData(powerRelayVO);
    }

    /*private void executeControl() {
        ControlResponseVO responseVO = amiClient.control();
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
    }*/

    private void sendReadData(PowerRelayVO powerRelayVO) {
        System.out.println(" [ ReadData ]\n->" + powerRelayVO);
//        List<String> errorCodes = amiClient.getErrorCodes();
//        DeviceVO pcsInfo = amiClient.getPcsInfo();

        //webSender.sendData(pcsInfo, pcsVO, errorCodes);
    }

    /*private void sendControlResponse(ControlResponseVO responseVO) {
        int result = responseVO.getResult();
        String remoteId = responseVO.getRequestVO().getRemoteId();

        webSender.sendResponse(remoteId, "control", result);
    }*/
}
