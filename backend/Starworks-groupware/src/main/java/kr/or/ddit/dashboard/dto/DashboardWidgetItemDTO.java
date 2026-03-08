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
public class DashboardWidgetItemDTO {

    private String itemType;
    private String title;
    private String subtitle;
    private String description;
    private String badge;
    private String route;
    private LocalDateTime createdAt;
}
