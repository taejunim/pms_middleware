package pms;

import pms.communication.CommunicationManager;
import pms.communication.external.smarthub.EVChargerClient;
import pms.scheduler.CommonScheduler;
import pms.system.PMSManager;
import pms.system.backup.BackupClient;
import pms.system.ess.ESSController;
import pms.system.ess.ESSScheduleManager;
import pms.vo.device.external.EVChargerVO;

import java.util.List;

public class Main {
    /**
     * PMS(Power Management System) Middleware
     *
     * @param args Arguments
     */
    public static void main(String[] args) {
        new PMSManager().initSystem();

        //new ESSController().setConfig();
        //new ESSScheduleManager().checkSchedule();

        //new CommonScheduler().startScheduler();

        //new BackupClient().connectSession();

        executeCommunication();

        /*EVChargerClient evChargerClient = new EVChargerClient();
        evChargerClient.request();

        List<EVChargerVO> chargers = evChargerClient.getEVChargers("ess-charge");
        System.out.println(chargers);*/
    }

    private static void executeCommunication() {
        CommunicationManager communication = new CommunicationManager();

        communication.executeWebsocket();
        communication.executeDevice();
    }
}