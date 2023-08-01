package pms.scheduler.external.switchboard;

import org.quartz.Job;
import org.quartz.JobDataMap;
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
        JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
        String meterCode = (String) jobDataMap.get("meterCode");
        executeCommunication(meterCode);
    }

    private void executeCommunication(String meterCode) {
        if (powerMeterClient.isConnected(meterCode)) {
            try {
                executeRead(meterCode);
            } finally {
                powerMeterClient.disconnect(meterCode);
            }
        } else {
            try {
                powerMeterClient.connect(meterCode);
            } catch (Exception e) {
                executeConnectionError(meterCode);
                powerMeterClient.disconnect(meterCode);

                e.printStackTrace();
            }
        }
    }

    private void executeRead(String meterCode) {
        PowerMeterVO powerMeterVO = powerMeterClient.read(meterCode);
        //sendReadData(powerRelayVO);
    }

    private void executeConnectionError(String meterCode) {
        PowerMeterVO powerMeterVO = powerMeterClient.readByError(meterCode);
        //sendReadData(powerRelayVO);
    }

}
