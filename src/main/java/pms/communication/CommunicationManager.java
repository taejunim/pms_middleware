package pms.communication;

import pms.common.util.ResourceUtil;
import pms.communication.device.bms.BMSClient;
import pms.communication.device.mobile.converter.ConverterClient;
import pms.communication.device.mobile.ioboard.IOBoardClient;
import pms.communication.device.pcs.PCSClient;
import pms.communication.web.WebClient;
import pms.vo.system.DeviceVO;
import pms.vo.system.PmsVO;

import java.net.URISyntaxException;
import java.util.Properties;

public class CommunicationManager {
    public static final Properties websocketProperties = ResourceUtil.loadProperties("websocket");
    public static final Properties deviceProperties = ResourceUtil.loadProperties("device");

    public void executeWebsocket() {
        try {
            WebClient webClient = new WebClient();
            webClient.execute();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public void executeDevice() {
        String essType = PmsVO.ess.getEssType();

        executeBMS();

        if (essType.equals("01")) {
            executePCS();
        } else if (essType.equals("02")) {
            executeConverter();
            executeIOBoard();
        }
    }

    private void executeBMS() {
        BMSClient bmsClient = new BMSClient();

        for (DeviceVO rackVO : PmsVO.racks) {
            String rackCode = rackVO.getDeviceCode();   //Rack 코드
            bmsClient.execute(rackCode, rackVO);
        }
    }

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
}
