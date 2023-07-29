package pms.scheduler.external.switchboard;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import pms.communication.external.switchboard.PowerMeterClient;
import pms.vo.system.PowerMeterVO;
/**
 * packageName    : pms.scheduler.external.switchboard
 * fileName       : PowerMeterJob
 * author         : youyeong
 * date           : 2023/07/28
 * description    : EV충전기 전력계측기 통신 Job
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2023/07/28        youyeong       최초 생성
 */
public class PowerMeterJob implements Job {
    private final PowerMeterClient powerMeterClient = new PowerMeterClient();

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        executeCommunication();
    }

    private void executeCommunication() {
        if (powerMeterClient.isConnected()) {
            try {
                System.out.println(" [ Read 시작 ] ");
                executeRead();
            } finally {
                powerMeterClient.disconnect();
            }
        } else {
            try {
                powerMeterClient.connect();
            } catch (Exception e) {
                executeConnectionError();
                powerMeterClient.disconnect();

                e.printStackTrace();
            }
        }
    }

    private void executeRead() {
        PowerMeterVO powerRelayVO = powerMeterClient.read();

        //sendReadData(powerRelayVO);
    }

    private void executeConnectionError() {
        PowerMeterVO powerRelayVO = powerMeterClient.readByError();
        //sendReadData(powerRelayVO);
    }

}
