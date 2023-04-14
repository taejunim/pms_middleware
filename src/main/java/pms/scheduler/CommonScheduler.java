package pms.scheduler;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 * Common Scheduler
 * <p>
 * - 공통 스케줄러
 */
public class CommonScheduler {
    private final SchedulerFactory schedulerFactory = new StdSchedulerFactory();    //Scheduler Factory

    /**
     * 스케줄러 시작 실헹
     */
    public void startScheduler() {
        try {
            Scheduler scheduler = schedulerFactory.getScheduler();  //Scheduler 정보 호출
            scheduler.start();  //Scheduler 시작
        } catch (SchedulerException e) {
            e.getLocalizedMessage();
        }
    }

    /**
     * 스케줄러 Job 종료 - Job, Trigger 정보 삭제
     *
     * @param scheduler  Scheduler
     * @param jobDataMap JobDataMap 정보
     * @return Scheduler Job 종료 여부
     */
    public static boolean stopJob(Scheduler scheduler, JobDataMap jobDataMap) {
        String jobName = jobDataMap.get("jobName").toString();  //Job 명
        String jobGroup = jobDataMap.get("jobGroup").toString();    //Job 그룹

        JobKey jobKey = JobKey.jobKey(jobName, jobGroup);   //Job Key

        try {
            //Job 존재 여부 확인
            if (scheduler.checkExists(jobKey)) {
                scheduler.deleteJob(jobKey);    //해당 Job 삭제

                return true;    //Scheduler 종료 여부
            }
        } catch (SchedulerException e) {
            e.getLocalizedMessage();
        }

        return false;   //Scheduler 종료 여부
    }

    /**
     * Job Detail 생성
     *
     * @param jobClass   스케줄러 실행할 Job Class
     * @param jobDataMap JobDataMap 정보
     * @return 생성된 Job Detail
     */
    public static JobDetail buildJobDetail(Class<? extends Job> jobClass, JobDataMap jobDataMap) {
        String jobName = jobDataMap.get("jobName").toString();  //Job 명
        String jobGroup = jobDataMap.get("jobGroup").toString();    //Job 그룹

        return newJob(jobClass)
                .withIdentity(jobName, jobGroup)    //Job Key 추가
                .usingJobData(jobDataMap)
                .build();
    }

    /**
     * Trigger 생성
     *
     * @param intervalSeconds 스케줄 실행 간격 시간(초)
     * @return 생성된 Trigger
     */
    public static Trigger buildTrigger(int intervalSeconds, JobDataMap jobDataMap) {
        String jobName = jobDataMap.get("jobName").toString();  //Job 명
        String jobGroup = jobDataMap.get("jobGroup").toString();    //Job 그룹
        String triggerName = jobDataMap.get("triggerName").toString();  //Trigger 명
        String triggerGroup = jobDataMap.get("triggerGroup").toString();    //Trigger 그룹

        return newTrigger()
                .withIdentity(triggerName, triggerGroup)    //Trigger Key 추가
                //.startAt(futureDate(2, IntervalUnit.SECOND))  //시작 시점
                .withSchedule(
                        simpleSchedule()
                                .withRepeatCount(1)
                                .withIntervalInSeconds(intervalSeconds) //매 초 주기
                                .repeatForever()    //계속 반복
                )
                .forJob(jobName, jobGroup)
                .build();
    }
}
