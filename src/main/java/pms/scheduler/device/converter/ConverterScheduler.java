package pms.scheduler.device.converter;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import pms.scheduler.CommonScheduler;
import pms.scheduler.device.pcs.PCSJob;

public class ConverterScheduler {
    private Scheduler scheduler;

    public ConverterScheduler() {
        try {
            SchedulerFactory schedulerFactory = new StdSchedulerFactory();
            scheduler = schedulerFactory.getScheduler();
        } catch (SchedulerException e) {
            e.getLocalizedMessage();
        }
    }

    public void execute() throws SchedulerException {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("jobGroup", "converter");
        jobDataMap.put("jobName", "converter-job");
        jobDataMap.put("triggerGroup", "converter");
        jobDataMap.put("triggerName", "converter-trigger");

        JobDetail jobDetail = CommonScheduler.buildJobDetail(ConverterJob.class, jobDataMap);
        scheduler.scheduleJob(jobDetail, CommonScheduler.buildTrigger(1, jobDataMap));
    }
}
