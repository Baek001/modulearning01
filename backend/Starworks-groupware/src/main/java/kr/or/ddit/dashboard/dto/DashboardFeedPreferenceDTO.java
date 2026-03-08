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
public class DashboardFeedPreferenceDTO {

    private String userId;
    private String defaultScope;
    private String defaultSort;
    private String defaultView;
    private String defaultCategory;
    private String lastDeptId;
    private String lastSearchQ;
    private LocalDateTime crtDt;
    private LocalDateTime lastChgDt;
}
