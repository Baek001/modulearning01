package kr.or.ddit.calendar.workspace.dto;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Data;

@Data
public class CalendarWorkspaceQuery {

    private String view;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate anchorDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    private String sourceGroups;
    private String q;
    private Long communityId;
    private String projectId;
    private String ownerUserId;
}
