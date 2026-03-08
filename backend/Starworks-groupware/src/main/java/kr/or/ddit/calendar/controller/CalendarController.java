package kr.or.ddit.calendar.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/calendar")
public class CalendarController {

    /**
     * 부서 일정 페이지로 이동합니다.
     * @param model
     * @return "calendar/calendar-depart"
     */
    @GetMapping("/depart")
    public String calendarDepart(Model model) {
        return "calendar/calendar-depart";
    }

    /**
     * 팀 일정 페이지로 이동합니다.
     * @param model
     * @return "calendar/calendar-team"
     */
    @GetMapping("/team")
    public String calendarTeam(Model model) {
        return "calendar/calendar-team";
    }
}
