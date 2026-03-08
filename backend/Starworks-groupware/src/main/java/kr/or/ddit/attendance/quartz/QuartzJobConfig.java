package kr.or.ddit.attendance.quartz;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzJobConfig {

    @Bean
    public JobDetail dailyJobDetail() {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("description", "매일 00:01 실행");

        return JobBuilder.newJob(DailySchedulerJob.class)
            .withIdentity("dailyJob", "dailyGroup")
            .withDescription("Daily Scheduler Job")
            .usingJobData(jobDataMap)
            .storeDurably()  // Trigger가 없어도 유지
            .build();
    }

    @Bean
    public Trigger dailyCronTrigger() {
        CronScheduleBuilder cronBuilder = CronScheduleBuilder
            .cronSchedule("0 01 0 * * ?")  // 매일 23:50
            .withMisfireHandlingInstructionFireAndProceed();

        return TriggerBuilder.newTrigger()
            .forJob(dailyJobDetail())
            .withIdentity("dailyCronTrigger", "dailyGroup")
            .withDescription("매일 00:01  실행")
            .withSchedule(cronBuilder)
            .build();
    }
}
