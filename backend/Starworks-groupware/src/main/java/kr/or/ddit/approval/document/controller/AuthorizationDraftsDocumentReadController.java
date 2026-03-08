package kr.or.ddit.approval.document.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import kr.or.ddit.approval.document.service.AuthorizationDocumentService;
import kr.or.ddit.approval.temp.service.AuthorizationTempService;
import kr.or.ddit.vo.AuthorizationTempVO;
import lombok.RequiredArgsConstructor;

/**
 * 임시저장함 READ 컨트롤러
 * @author 임가영
 * @since 2025. 10. 3.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 3.     	임가영	        최초 생성
 *  2025. 10. 24.    	홍현택	        카운트 로직 분리
 *
 * </pre>
 */
@Controller
@RequiredArgsConstructor
public class AuthorizationDraftsDocumentReadController {

	@Autowired
	private AuthorizationTempService authorizationTempService;
	private final AuthorizationDocumentService service;
	private static final int screenSize = 5;
    private static final int blockSize = 5;

	/**
	 * 임시저장함 페이지로 이동
	 * @return
	 */
	@GetMapping("/approval/drafts")
	public String readAuthorizationdDraftsDocumentRead(
			Model model,
			Authentication authentication){

		String loginId = authentication.getName();

        // 임시저장 목록 조회
        List<AuthorizationTempVO> draftList = authorizationTempService.readAuthTempList();
        model.addAttribute("draftList", draftList);

		return "approval/approvaldrafts";
	}
}
