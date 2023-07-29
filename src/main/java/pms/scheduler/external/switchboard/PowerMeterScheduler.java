package pms.scheduler.external.switchboard;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import pms.scheduler.CommonScheduler;
/**
 * packageName    : pms.scheduler.external.switchboard
 * fileName       : PowerMeterScheduler
 * author         : youyeong
 * date           : 2023/07/28
 * description    : EV충전기 전력계측기 통신 Scheduler
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2023/07/28        youyeong       최초 생성
 */
public class PowerMeterScheduler {

    private Scheduler scheduler;
    public PowerMeterScheduler() {
        try {
            SchedulerFactory schedulerFactory = new StdSchedulerFactory();
            scheduler = schedulerFactory.getScheduler();
        } catch (SchedulerException e) {
            e.getLocalizedMessage();
        }
    }

    public void execute() throws SchedulerException {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("jobGroup", "powerMeter");
        jobDataMap.put("jobName", "powerMeter-job");
        jobDataMap.put("triggerGroup", "powerMeter");
        jobDataMap.put("triggerName", "powerMeter-trigger");

        JobDetail jobDetail = CommonScheduler.buildJobDetail(PowerMeterJob.class, jobDataMap);
        scheduler.scheduleJob(jobDetail, CommonScheduler.buildTrigger(1, jobDataMap));
    }
}
