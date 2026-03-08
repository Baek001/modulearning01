package kr.or.ddit.attendance.quartz;

import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class StartupJobExecutor {

    private final Scheduler scheduler;

    @PostConstruct
    public void executeOnStartup() {
        try {
            log.info("서버 시작 - 스케줄러 즉시 실행");
            scheduler.triggerJob(JobKey.jobKey("dailyJob", "dailyGroup"));
        } catch (Exception e) {
            log.error("서버 시작 시 스케줄러 실행 실패", e);
        }
    }
}
