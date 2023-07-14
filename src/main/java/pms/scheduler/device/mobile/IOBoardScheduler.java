package pms.scheduler.device.mobile;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import pms.scheduler.CommonScheduler;

public class IOBoardScheduler {
    public Scheduler scheduler;

    public static boolean writeFlag = false;        // 제어 플래그 (임시 위치, 추후 VO로 이동)
    public static int deviceIndex = 0;              // 제어 장비 번호 (임시 위치, 추후 VO로 이동)
    public static boolean deviceStatus = false;     // 제어 장비 상태 : ON(T)/OFF(F) (임시 위치, 추후 VO로 이동)

    public IOBoardScheduler() {
        try {
            SchedulerFactory schedulerFactory = new StdSchedulerFactory();
            scheduler = schedulerFactory.getScheduler();
        } catch (SchedulerException e) {
            e.printStackTrace();
            //System.out.println(new Logger().deviceLog("error", "IOBoard", "Scheduler Error: " + e.getLocalizedMessage()));
        }
    }


    /**
     * Read Scheduler job 실행 부
     *
     * @throws SchedulerException
     */
    public void execute() throws SchedulerException {
        //System.out.println(new Logger().deviceLog("complete", "Battery", "Rack(" + rackNo + ") Read Job Start"));

        JobDataMap jobDataMap = new JobDataMap();

        jobDataMap.put("jobGroup", "IOBoard");
        jobDataMap.put("jobName", "IOBoardJob");
        jobDataMap.put("triggerGroup", "IOBoard");
        jobDataMap.put("triggerName", "IOBoardTrigger");

        JobDetail jobDetail = CommonScheduler.buildJobDetail(IOBoardJob.class, jobDataMap);
        scheduler.scheduleJob(jobDetail, CommonScheduler.buildTrigger(2, jobDataMap));
    }
}
