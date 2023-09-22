package pms.scheduler.device.airconditioner;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import pms.scheduler.CommonScheduler;
import pms.vo.device.AirConditionerVO;

public class AirConditionerScheduler {
    private Scheduler scheduler;

    public AirConditionerScheduler() {
        try {
            SchedulerFactory schedulerFactory = new StdSchedulerFactory();
            scheduler = schedulerFactory.getScheduler();
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    public void execute(String airConditionerCode) throws Exception {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("airConditionerCode", airConditionerCode);
        jobDataMap.put("jobGroup", "airConditioner");
        jobDataMap.put("jobName", "airConditioner-job");
        jobDataMap.put("triggerGroup", "airConditioner");
        jobDataMap.put("triggerName", "airConditioner-trigger");

        JobDetail jobDetail = CommonScheduler.buildJobDetail(AirConditionerJob.class, jobDataMap);
        scheduler.scheduleJob(jobDetail, CommonScheduler.buildTrigger(1, jobDataMap));
    }
}
