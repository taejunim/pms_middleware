package pms.scheduler.external.switchboard.relay;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import pms.scheduler.CommonScheduler;

/**
 * packageName    : pms.scheduler.external.switchboard
 * fileName       : PowerRelayScheduler
 * author         : tjlim
 * date           : 2023/07/25
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2023/07/25        tjlim       최초 생성
 */
public class PowerRelayScheduler {
    private Scheduler scheduler;

    public PowerRelayScheduler() {
        try {
            SchedulerFactory schedulerFactory = new StdSchedulerFactory();
            scheduler = schedulerFactory.getScheduler();
        } catch (SchedulerException e) {
            e.getLocalizedMessage();
        }
    }

    public void execute() throws SchedulerException {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("jobGroup", "powerRelay");
        jobDataMap.put("jobName", "powerRelay-job");
        jobDataMap.put("triggerGroup", "powerRelay");
        jobDataMap.put("triggerName", "powerRelay-trigger");

        JobDetail jobDetail = CommonScheduler.buildJobDetail(PowerRelayJob.class, jobDataMap);
        scheduler.scheduleJob(jobDetail, CommonScheduler.buildTrigger(1, jobDataMap));
    }
}
