package kr.or.ddit.approval.controller;

import java.time.LocalDate;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import kr.or.ddit.approval.document.service.AuthorizationDocumentService;
import kr.or.ddit.approval.temp.service.AuthorizationTempService;
import kr.or.ddit.approval.template.service.AuthorizationDocumentTemplateService;
import kr.or.ddit.vo.AuthorizationDocumentTemplateVO;
import kr.or.ddit.vo.AuthorizationDocumentVO;
import kr.or.ddit.vo.AuthorizationTempVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author 윤서현
 * @since 2025. 10. 1.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 1.     	윤서현	        최초 생성
 *	2025.10. 02.		임가영			보완 주석 추가
 *	2025.10. 02.        윤서현			첨부파일 메서드 추가
 *  2025.10. 10.		임가영			getNextDocId 메소드 restController 로 옮김
 * </pre>
 */
@RequestMapping("/approval")
@Controller
@RequiredArgsConstructor
@Slf4j
public class ApprovalCreateController {

	private final AuthorizationDocumentService docService;
	private final AuthorizationDocumentTemplateService tmplService;
	private final AuthorizationTempService tempService;
	
	@GetMapping
	public String createDraft(
		@RequestParam("formId") String fromId, 
		@RequestParam(required = false) String atrzTempSqn,  
		Model model
	) {
		
		if (atrzTempSqn != null) {
	        //  임시저장 불러오기 로직
	        AuthorizationTempVO temp = tempService.readAuthTemp(atrzTempSqn); // mapperTemp.selectAuthTemp 호출
	        model.addAttribute("template", temp);
	        model.addAttribute("atrzTempSqn", temp.getAtrzTempSqn());
	        model.addAttribute("htmlData", temp.getHtmlData());
	        model.addAttribute("today", LocalDate.now());
	        model.addAttribute("docNo", "임시저장 문서");
	    } else if (fromId != null) {
	        //  기존 신규 양식 로직 그대로
	        AuthorizationDocumentTemplateVO template = tmplService.readAuthDocumentTemplate(fromId);
	        model.addAttribute("template", template);
	        model.addAttribute("atrzTempSqn", ""); //
	        model.addAttribute("today", LocalDate.now());
	        model.addAttribute("docNo", "임시문서번호");
	    }
		
		return "approval/approval-draft-create";
	}
	
	
	@PostMapping("create")
	public String approvalCreate(
		@ModelAttribute AuthorizationDocumentVO authDoc,
		@RequestParam(name = "atrzTempSqn", required = false) String atrzTempSqn,
		Authentication authentication,
		RedirectAttributes redirectAttributes
	)throws Exception{
		
		// 로그인 사용자 ID 세팅
		String userName = authentication.getName();
		authDoc.setAtrzUserId(userName);
		
		//AuthorizationDocumentVO authDocumentVo = docService.insertAuthDocument(authDoc);

		if (atrzTempSqn != null && !atrzTempSqn.isEmpty()) {
	        tempService.deleteAuthTemp(atrzTempSqn);
	        log.info("임시저장 문서({}) 삭제 완료", atrzTempSqn);
	    }
		
		redirectAttributes.addFlashAttribute("icon", "success");
		redirectAttributes.addFlashAttribute("alertMessage", "결재요청이 완료되었습니다.");
		
		docService.insertAuthDocument(authDoc);
		return "redirect:/approval/detail/" + authDoc.getAtrzDocId();
	}
	
}
