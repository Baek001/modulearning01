package kr.or.ddit.approval.controller;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 
 * @author 홍현택
 * @since 2025. 9. 30.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 30.     	홍현택	          결재 뷰 이동 주석처리 --> /kr/or/ddit/approval/controller/ApprovalReadController.java
 *
 * </pre>
 */
//@Controller
@RequestMapping("/approval")
public class ApprovalController {

//    /**
//     * 결재 메인 페이지로 이동합니다.
//     * @param model
//     * @return "approval/approvalMain3"
//     */
//    @GetMapping("/main")
//    public String approvalMain(Model model) {
//        model.addAttribute("currentMenu", "approval");
//        model.addAttribute("currentSubMenu", "myApprovals");
//        return "approval/approval-main";
//    }

//    /**
//     * 내 기안 문서 페이지로 이동합니다.
//     * @param model
//     * @return "approval/approvaldrafts"
//     */
//    @GetMapping("/drafts")
//    public String approvalDrafts(Model model) {
//        model.addAttribute("currentMenu", "approval");
//        model.addAttribute("currentSubMenu", "drafts");
//        return "approval/approvaldrafts";
//    }

//    /**
//     * 결재 보관함 페이지로 이동합니다.
//     * @param model
//     * @return "approval/approvalarchive"
//     */
//    @GetMapping("/archive")
//    public String approvalArchive(Model model) {
//        model.addAttribute("currentMenu", "approval");
//        model.addAttribute("currentSubMenu", "archive");
//        return "approval/approvalarchive";
//    }
    
//    /**
//     * 기안서 작성 페이지로 이동합니다.
//     * @param model
//     * @return "approval/approval-draft-create"
//     */
//    @GetMapping("/draft/create")
//    public String approvalDraftCreate(Model model) {
//        model.addAttribute("currentMenu", "approval");
//        model.addAttribute("currentSubMenu", "createDraft");
//        return "approval/approval-draft-create";
//    }
    
    /**
     * 지출결의서 작성 페이지로 이동합니다.
     * @param model
     * @return "approval/approval-expense-create"
     */
    @GetMapping("/expense/create")
    public String approvalExpenseCreate(Model model) {
        model.addAttribute("currentMenu", "approval");
        model.addAttribute("currentSubMenu", "createExpense");
        return "approval/approval-expense-create";
    }
    
    /**
     * 지출결의서 조회 페이지로 이동합니다.
     * @param model
     * @return "approval/approval-expense-view"
     */
    @GetMapping("/expense/view")
    public String approvalExpenseView(Model model) {
        model.addAttribute("currentMenu", "approval");
        model.addAttribute("currentSubMenu", "viewExpense");
        return "approval/approval-expense-view";
    }
    
    /**
     * 업무보고서 조회 페이지로 이동합니다.
     * @param model
     * @return "approval/approval-report-view"
     */
    @GetMapping("/report/view")
    public String approvalReportView(Model model) {
        model.addAttribute("currentMenu", "approval");
        model.addAttribute("currentSubMenu", "viewReport");
        return "approval/approval-report-view";
    }
    
    /**
     * 지출 결의서 기안서 작성폼으로 이동합니다.
     * @param model
     * @return "approval/approval-expense-create"
     */
    @GetMapping("/spend/create")
    public String leaveCreate(Model model) {
    	model.addAttribute("currentMenu", "approval");
        model.addAttribute("currentSubMenu", "viewReport");
        return "approval/approval-expense-create"; 
      
    }
    
    
    /**
     * 기안서 작성폼으로 이동합니다.
     * @param model
     * @return "approval/approval-draft-create"
     */
    @GetMapping("/basic/create")
    public String BasicCreate(Model model) {
    	model.addAttribute("currentMenu", "approval");
    	model.addAttribute("currentSubMenu", "viewReport");
    	return "approval/approval-draft-create"; 
    	
    }
    
    
}
