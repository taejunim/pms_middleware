package pms.communication;

import pms.common.util.ResourceUtil;
import pms.communication.device.airconditioner.AirConditionerClient;
import pms.communication.device.bms.BMSClient;
import pms.communication.device.converter.ConverterClient;
import pms.communication.device.mobile.ioboard.IOBoardClient;
import pms.communication.device.pcs.PCSClient;
import pms.communication.external.switchboard.meter.PowerMeterClient;
import pms.communication.external.switchboard.relay.PowerRelayClient;
import pms.communication.web.WebClient;
import pms.system.backup.BackupClient;
import pms.vo.system.DeviceVO;
import pms.vo.system.PmsVO;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class CommunicationManager {
    public static final Properties deviceProperties = ResourceUtil.loadProperties("device");

    public void executeWebsocket() {
        try {
            WebClient webClient = new WebClient();
            webClient.execute();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public void executeBackup() {
        BackupClient backupClient = new BackupClient();
        backupClient.execute();
    }

    public void executeDevice() {
        String essType = PmsVO.ess.getEssType();
        System.out.println("ESS 유형 : " + essType);

        executeBMS();

        if (essType.equals("01")) {
            executePCS();
            executePowerRelay();
            executePowerMeter();
            //executeAirConditioner();
        } else if (essType.equals("02")) {
            executeConverter();
            //executeIOBoard();
        }
    }

    /**
     * BMS 통신 실행
     */
    private void executeBMS() {
        BMSClient bmsClient = new BMSClient();

        for (DeviceVO rackVO : PmsVO.racks) {
            String rackCode = rackVO.getDeviceCode();   //Rack 코드
            bmsClient.execute(rackCode, rackVO);
        }
    }

    /**
     * PCS 통신 실행
     */
    private void executePCS() {
        PCSClient pcsClient = new PCSClient();
        pcsClient.execute();
    }

    private void executeConverter() {
        ConverterClient converterClient = new ConverterClient();
        converterClient.execute();
    }

    private void executeIOBoard() {
        IOBoardClient ioBoardClient = new IOBoardClient();
        ioBoardClient.execute();
    }

    private void executePowerRelay() {
        PowerRelayClient powerRelayClient = new PowerRelayClient();
        powerRelayClient.execute();
    }

    private void executePowerMeter() {
        PowerMeterClient powerMeterClient = new PowerMeterClient();

        for (DeviceVO rackVO : PmsVO.meters.get("0502")) {
            powerMeterClient.execute(rackVO);
        }
    }

    private void executeAirConditioner() {
        AirConditionerClient airConditionerClient = new AirConditionerClient();

        for (Map.Entry<String, List<DeviceVO>> airConditionerMap : PmsVO.airConditioners.entrySet()) {
            for (DeviceVO airConditionerVO : airConditionerMap.getValue()) {
                String airConditionerDeviceCode = airConditionerVO.getDeviceCode();
                airConditionerClient.execute(airConditionerDeviceCode, airConditionerVO);
            }
        }
    }
}
