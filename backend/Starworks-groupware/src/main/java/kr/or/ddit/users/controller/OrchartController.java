package kr.or.ddit.users.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/orgchart")
public class OrchartController {

    /**
     * 조직도 사용자 페이지로 이동합니다.
     * @param model
     * @return "orgchart/orgchart-user"
     */
    @GetMapping("/user")
    public String orgchartUser(Model model) {
        model.addAttribute("currentMenu", "orgchart");
        model.addAttribute("currentSubMenu", "user");
        return "orgchart/orgchart-user";
    }

    /**
     * 조직도 사용자 V2 페이지로 이동합니다.
     * @param model
     * @return "orgchart/orgchart-user-v2"
     */
    @GetMapping("/user/v2")
    public String orgchartUserV2(Model model) {
        model.addAttribute("currentMenu", "orgchart");
        model.addAttribute("currentSubMenu", "userV2");
        return "orgchart/orgchart-user-v2";
    }
       
}
