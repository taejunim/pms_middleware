package pms.scheduler.backup;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import pms.scheduler.CommonScheduler;
import pms.scheduler.device.bms.BMSJob;
import pms.scheduler.device.pcs.PCSJob;

/**
 * packageName    : pms.scheduler.backup
 * fileName       : BackupScheduler
 * author         : tjlim
 * date           : 2023/08/02
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2023/08/02        tjlim       최초 생성
 */
public class BackupScheduler {
    private Scheduler scheduler;

    public BackupScheduler() {
        try {
            SchedulerFactory schedulerFactory = new StdSchedulerFactory();
            scheduler = schedulerFactory.getScheduler();
        } catch (SchedulerException e) {
            e.getLocalizedMessage();
        }
    }

    public void execute(String backupDate) throws SchedulerException {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("jobGroup", "backup-" + backupDate);
        jobDataMap.put("jobName", "backup-" + backupDate + "-job");
        jobDataMap.put("triggerGroup", "backup-" + backupDate);
        jobDataMap.put("triggerName", "backup-" + backupDate + "-trigger");

        JobDetail jobDetail = CommonScheduler.buildJobDetail(BackupJob.class, jobDataMap);
        scheduler.scheduleJob(jobDetail, CommonScheduler.buildCronTrigger(backupDate, jobDataMap));
    }
}
