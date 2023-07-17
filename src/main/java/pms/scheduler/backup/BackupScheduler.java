package pms.scheduler.backup;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import pms.scheduler.CommonScheduler;
import pms.scheduler.device.bms.BMSJob;

public class BackupScheduler {
    private Scheduler scheduler;

    public BackupScheduler() {
        try {
            SchedulerFactory schedulerFactory = new StdSchedulerFactory();
            scheduler = schedulerFactory.getScheduler();
        } catch (SchedulerException e) {
            //e.getLocalizedMessage();
            e.printStackTrace();
        }
    }

    public void executeUpload(String backupDate) throws SchedulerException {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("jobGroup", "backup-" + backupDate);
        jobDataMap.put("jobName", "backup-" + backupDate + "-job");
        jobDataMap.put("triggerGroup", "backup-" + backupDate);
        jobDataMap.put("triggerName", "backup-" + backupDate + "-trigger");

        JobDetail jobDetail = CommonScheduler.buildJobDetail(BMSJob.class, jobDataMap);
        scheduler.scheduleJob(jobDetail, CommonScheduler.buildTrigger(3, jobDataMap));
    }
}
