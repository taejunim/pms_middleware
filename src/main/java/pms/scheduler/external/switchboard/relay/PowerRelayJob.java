package pms.scheduler.external.switchboard.relay;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import pms.communication.external.switchboard.relay.PowerRelayClient;
import pms.vo.device.external.PowerRelayVO;

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

    private void sendReadData(PowerRelayVO powerRelayVO) {
        System.out.println(" [ ReadData ]\n->" + powerRelayVO);
//        List<String> errorCodes = amiClient.getErrorCodes();
//        DeviceVO pcsInfo = amiClient.getPcsInfo();

        //webSender.sendData(pcsInfo, pcsVO, errorCodes);
    }
}
