package pms;

import pms.communication.CommunicationManager;
import pms.scheduler.CommonScheduler;
import pms.system.PMSManager;
import pms.system.backup.BackupClient;
import pms.system.ess.ESSController;
import pms.system.ess.ESSScheduleManager;
import pms.vo.system.PmsVO;

public class Main {
    /**
     * PMS(Power Management System) Middleware
     *
     * @param args Arguments
     */
    public static void main(String[] args) {
        new PMSManager().initSystem();
        new CommonScheduler().startScheduler();

        String essType = PmsVO.ess.getEssType();

        //ESS 유형에 따른 기능 실행
        if (essType.equals("01")) {
            new ESSController().setConfig();
            new ESSScheduleManager().checkSchedule();
        } else if (essType.equals("02")) {

        }

        executeCommunication();
    }

    /**
     * 통신 실행
     */
    private static void executeCommunication() {
        CommunicationManager communication = new CommunicationManager();

        communication.executeWebsocket();
        communication.executeDevice();
    }
}