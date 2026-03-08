package kr.or.ddit.approval.template.controller;

import java.time.LocalDate;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import kr.or.ddit.approval.document.service.AuthorizationDocumentService;
import kr.or.ddit.approval.temp.service.AuthorizationTempService;
import kr.or.ddit.approval.template.service.AuthorizationDocumentTemplateService;
import kr.or.ddit.project.mngt.service.projectService;
import kr.or.ddit.security.CustomUserDetails;
import kr.or.ddit.vo.AuthorizationDocumentTemplateVO;
import kr.or.ddit.vo.AuthorizationTempVO;
import kr.or.ddit.vo.UsersVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author 윤서현
 * @since 2025. 9. 29.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 29.     	윤서현	          최초 생성
 *  2025. 9. 30.  		김주민 			projectService, bizId 파라미터 추가
 *  2025.10. 02.		임가영			보완 주석 추가
 *  2025.10. 10.		임가영			docService.getNextDocId() (DB에서 시퀀스 가져오는 로직) 삭제
 * </pre>
 */
@Controller
@RequestMapping("/approval")
@RequiredArgsConstructor
@Slf4j
public class AuthorizationDocumentTemplateCreateController {
	
	private final AuthorizationDocumentTemplateService service;
	private final projectService projectService;
	private final AuthorizationTempService authorizationTempService;
	private final AuthorizationDocumentService docService;
	
	@GetMapping("create")
	public String createAuthorizationDocumentTemplate(
		@RequestParam(value = "formId", required = false) String formId,
		@RequestParam(value = "bizId", required = false) String bizId,
		@RequestParam(value = "atrzTempSqn", required = false) String atrzTempSqn,
		Model model,
		@AuthenticationPrincipal CustomUserDetails customUser
	) {
		if (formId == null || formId.isBlank()) {
			return "redirect:/approval/main";
		}

		// 기안서 템플릿 로드
		AuthorizationDocumentTemplateVO template = service.readAuthDocumentTemplate(formId);
		
		// 임시저장된 문서가 있을 경우 불러오기
	    if (atrzTempSqn != null && !atrzTempSqn.isEmpty()) {
	        AuthorizationTempVO tempData = authorizationTempService.readAuthTemp(atrzTempSqn);

	        // 로그인한 사용자 검증 (보안)
	        if (!tempData.getAtrzUserId().equals(customUser.getRealUser().getUserId())) {
	            return "error/403"; // 본인 문서가 아니면 접근 차단
	        }

	        // 임시저장된 HTML_DATA 로 대체
	        template.setHtmlContents(tempData.getHtmlData());
	        model.addAttribute("atrzTempSqn", atrzTempSqn); // 나중에 덮어쓰기 저장할 때 사용
	        model.addAttribute("tempTitle", tempData.getAtrzDocTtl());
	    }
		
		// bizId가 있으면 프로젝트 데이터로 치환
	    if (bizId != null && !bizId.isEmpty()) {
	        try {
	            // Service에서 프로젝트 데이터를 템플릿용 Map으로 변환
	            Map<String, String> projectData = projectService.convertProjectToTemplateData(bizId);
	            
	            String content = template.getHtmlContents();
	            
	            // 모든 html 플레이스홀더 치환
	            for (Map.Entry<String, String> entry : projectData.entrySet()) {
	                content = content.replace(entry.getKey(), entry.getValue());
	            }
	            template.setHtmlContents(content);
	        } catch (Exception e) {
	            log.error("프로젝트 ID {} 로드 실패: {}", bizId, e.getMessage());
	        }
	        
	        // ** ApprovalCreateController(기안서작성 페이지로 이동하는 컨트롤러)와 합치지 않고 이대로?
	        // ** 현재 경로는 프로젝트 기안서 형식만 오기때문에 formId 쿼리스트링 필요 없음.
	        // 1. 프로젝트 기안서 템플릿 로드
	        // 2. 프로젝트 Id 로 승인 대기 중인 데이터 풀러오기
	        // 3. 데이터를 html 로 치환 후 template 에 저장
	        // 4. template 내보내기
	    }
	    
	    model.addAttribute("template", template);
	    
	    // 사용자 정보 VO
	    UsersVO loginUser = customUser.getRealUser();
	    model.addAttribute("user", loginUser);
	    model.addAttribute("today", LocalDate.now());
	  
		return "approval/approval-draft-create";
		
	}
	
}
