package pms;

import pms.communication.CommunicationManager;
import pms.scheduler.CommonScheduler;
import pms.system.PMSManager;
import pms.system.ess.ESSController;
import pms.system.ess.ESSScheduleManager;
import pms.system.ess.NotificationService;
import pms.vo.system.PmsVO;

public class Main {
    /**
     * PMS(Power Management System) Middleware
     *
     * @param args Arguments
     */
    public static void main(String[] args) {
        /*float totalApparentPower = (float) 0.233318;

        if (totalApparentPower > 0.1) {
            System.out.println((Math.ceil(totalApparentPower * 10) / 10.0));
        } else {
            System.out.println((Math.round(totalApparentPower * 10) / 10.0));
        }


        float chargerPower = (float) (Math.ceil(totalApparentPower * 10) / 10.0);

        float test2 = Math.round(totalApparentPower * 100);

        float test1 = (float) (Math.round(totalApparentPower * 10) / 10.0);

        System.out.println("[전력 계측기] 총 피상 전력 = " + test1);
        System.out.println("[전력 계측기] 총 피상 전력 = " + Math.ceil(test1 * 10) / 10.0);
        System.out.println("[전력 계측기] 총 피상 전력 = " + Math.ceil(test1 * 100) / 100.0);
        System.out.println("[전력 계측기] 충전기 소비 전력 = " + chargerPower);*/

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
        communication.executeBackup();
        communication.executeDevice();
    }
}