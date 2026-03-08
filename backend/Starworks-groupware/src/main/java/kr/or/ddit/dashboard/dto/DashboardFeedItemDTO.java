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
public class DashboardFeedItemDTO {

    private String feedId;
    private String itemType;
    private String activityType;
    private String categoryCode;
    private String categoryLabel;
    private LocalDateTime createdAt;
    private String actorUserId;
    private String actorName;
    private String actorFilePath;
    private String actorDeptId;
    private String actorDeptName;
    private String actorJobGradeName;
    private String title;
    private String bodyPreview;
    private Integer commentCount;
    private Integer viewCount;
    private Integer reactionScore;
    private String route;
    private String badge;
    private String visibility;
    private Boolean read;
    private Boolean saved;
    private Boolean mine;
    private Boolean commented;
}
