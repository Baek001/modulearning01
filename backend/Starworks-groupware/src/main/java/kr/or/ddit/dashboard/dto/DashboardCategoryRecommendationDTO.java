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
public class DashboardCategoryRecommendationDTO {

    private Long recommendId;
    private String fromUserId;
    private String fromUserName;
    private String fromDeptName;
    private String toUserId;
    private String toUserName;
    private String bbsCtgrCd;
    private String categoryLabel;
    private String message;
    private String acceptedYn;
    private String readYn;
    private LocalDateTime crtDt;
    private LocalDateTime acceptedDt;
}
