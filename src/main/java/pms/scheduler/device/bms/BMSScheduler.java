package pms.scheduler.device.bms;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import pms.scheduler.CommonScheduler;

/**
 * BMS Scheduler
 * <p>
 * - BMS 통신 스케줄러 관리
 */
public class BMSScheduler {
    private Scheduler scheduler;

    public BMSScheduler() {
        try {
            SchedulerFactory schedulerFactory = new StdSchedulerFactory();
            scheduler = schedulerFactory.getScheduler();
        } catch (SchedulerException e) {
            e.getLocalizedMessage();
        }
    }

    /**
     * BMS 통신 스케줄러 Job 생성 및 실행
     *
     * @param rackCode Rack 코드
     * @throws SchedulerException SchedulerException
     */
    public void execute(String rackCode) throws SchedulerException {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("rackCode", rackCode);
        jobDataMap.put("jobGroup", "Rack-" + rackCode);
        jobDataMap.put("jobName", "Rack-" + rackCode + "-Job");
        jobDataMap.put("triggerGroup", "Rack-" + rackCode);
        jobDataMap.put("triggerName", "Rack-" + rackCode + "-Trigger");

        JobDetail jobDetail = CommonScheduler.buildJobDetail(BMSJob.class, jobDataMap);
        scheduler.scheduleJob(jobDetail, CommonScheduler.buildTrigger(3, jobDataMap));
    }
}
