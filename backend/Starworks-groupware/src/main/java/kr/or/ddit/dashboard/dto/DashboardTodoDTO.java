package kr.or.ddit.dashboard.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardTodoDTO {

    private Long todoId;
    private String userId;
    private String targetUserId;
    private String targetUserName;
    private String targetDeptName;
    private String targetJobGradeName;
    private String todoTtl;
    private String todoCn;
    private String doneYn;
    private LocalDateTime dueDt;
    private LocalDateTime crtDt;
    private LocalDateTime lastChgDt;
}
