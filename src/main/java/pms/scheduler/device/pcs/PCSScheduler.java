package pms.scheduler.device.pcs;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import pms.scheduler.CommonScheduler;

public class PCSScheduler {
    private Scheduler scheduler;

    public PCSScheduler() {
        try {
            SchedulerFactory schedulerFactory = new StdSchedulerFactory();
            scheduler = schedulerFactory.getScheduler();
        } catch (SchedulerException e) {
            e.getLocalizedMessage();
        }
    }

    public void execute() throws SchedulerException {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("jobGroup", "PCS");
        jobDataMap.put("jobName", "PCS-Job");
        jobDataMap.put("triggerGroup", "PCS");
        jobDataMap.put("triggerName", "PCS-Trigger");

        JobDetail jobDetail = CommonScheduler.buildJobDetail(PCSJob.class, jobDataMap);
        scheduler.scheduleJob(jobDetail, CommonScheduler.buildTrigger(1, jobDataMap));
    }
}
