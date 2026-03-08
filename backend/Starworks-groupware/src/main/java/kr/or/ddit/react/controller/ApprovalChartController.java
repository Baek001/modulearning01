package kr.or.ddit.react.controller;

import kr.or.ddit.react.service.ApprovalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rest/approval-chart-data")
public class ApprovalChartController {

    @Autowired
    private ApprovalService approvalService;

    @GetMapping("/monthly-usage")
    public List<Map<String, Object>> getMonthlyApprovalUsage() {
        return approvalService.getMonthlyApprovalUsageByCategory();
    }
}
