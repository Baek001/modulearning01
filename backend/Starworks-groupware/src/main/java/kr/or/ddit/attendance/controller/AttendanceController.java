package kr.or.ddit.attendance.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/attendance")
public class AttendanceController {

    /**
     * 근태관리 메인 페이지로 이동합니다.
     * @param model
     * @return "attendance/attendance-main"
     */
    @GetMapping("/main")
    public String attendanceMain(Model model) {
        return "attendance/attendance-main";
    }
    
    @GetMapping("/depart")
    public String attendanceDepart(Model model) {
    	return "attendance/attendance-depart";
    }
}
