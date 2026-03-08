package kr.or.ddit.calendar.workspace.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CalendarWorkspaceResponse {

    LocalDate startDate;
    LocalDate endDate;
    List<CalendarWorkspaceEventDTO> items;
}
