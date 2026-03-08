package kr.or.ddit.board.workspace.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BoardWorkspaceScheduler {

    private final BoardWorkspaceService boardWorkspaceService;

    @Scheduled(cron = "0 * * * * *")
    public void publishDueScheduledPosts() {
        boardWorkspaceService.publishDueScheduledPosts();
    }
}
