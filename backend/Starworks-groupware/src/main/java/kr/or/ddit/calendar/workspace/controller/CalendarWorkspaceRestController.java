package kr.or.ddit.calendar.workspace.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kr.or.ddit.calendar.workspace.dto.CalendarWorkspaceQuery;
import kr.or.ddit.calendar.workspace.dto.CalendarWorkspaceResponse;
import kr.or.ddit.calendar.workspace.service.CalendarWorkspaceService;
import kr.or.ddit.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/rest/calendar")
@RequiredArgsConstructor
public class CalendarWorkspaceRestController {

    private final CalendarWorkspaceService calendarWorkspaceService;

    @GetMapping("/events")
    public CalendarWorkspaceResponse readWorkspaceEvents(
        @ModelAttribute CalendarWorkspaceQuery query,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return calendarWorkspaceService.readWorkspaceEvents(query, userDetails);
    }
}
