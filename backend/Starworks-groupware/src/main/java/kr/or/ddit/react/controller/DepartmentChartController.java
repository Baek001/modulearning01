package kr.or.ddit.react.controller;

import kr.or.ddit.react.service.DepartmentForService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
*
* @author 홍현택
* @since 2025. 10. 17.
* @see
*
* <pre>
* << 개정이력(Modification Information) >>
*
*   수정일      			수정자           수정내용
*  -----------   	-------------    ---------------------------
*  2025. 10. 17.     	홍현택	          최초 생성
*
* </pre>
*/
@RestController
@RequestMapping("/rest/department-chart-data")
public class DepartmentChartController {

    @Autowired
    private DepartmentForService departmentForService;

    @GetMapping("/user-counts")
    public List<Map<String, Object>> getDepartmentUserCounts() {
        return departmentForService.getDepartmentUserCounts();
    }
}
