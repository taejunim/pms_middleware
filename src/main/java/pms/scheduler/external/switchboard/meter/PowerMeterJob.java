package pms.scheduler.external.switchboard.meter;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import pms.communication.external.switchboard.meter.PowerMeterClient;
import pms.communication.web.WebSender;
import pms.system.ess.ESSManager;
import pms.vo.device.external.PowerMeterVO;
import pms.vo.system.DeviceVO;

import java.util.List;

/**
 * Power Meter Job
 * <p>
 * - 전력 계측기 Scheduler Job
 */
public class PowerMeterJob implements Job {
    private final PowerMeterClient powerMeterClient = new PowerMeterClient();
    private final WebSender webSender = new WebSender();

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
        sendReadData(meterCode, powerMeterVO);

        ESSManager essManager = new ESSManager();
        essManager.saveEVChargerPower(meterCode, powerMeterVO.getTotalApparentPower());
    }

    private void executeConnectionError(String meterCode) {
        PowerMeterVO powerMeterVO = powerMeterClient.readByError(meterCode);
        sendReadData(meterCode, powerMeterVO);
    }

    private void sendReadData(String meterCode, PowerMeterVO powerMeterVO) {
        DeviceVO meterInfo = powerMeterClient.getMeterInfo(meterCode);
        List<String> errorCodes = powerMeterClient.getErrorCodes(meterCode);

        webSender.sendData(meterInfo, powerMeterVO, errorCodes);
    }
}
