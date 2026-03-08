package kr.or.ddit.approval.temp.controller;

import java.time.LocalDateTime;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import kr.or.ddit.approval.temp.service.AuthorizationTempService;
import kr.or.ddit.vo.AuthorizationTempVO;
import lombok.RequiredArgsConstructor;

/**
 * 
 * @author 윤서현
 * @since 2025. 10. 7.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 7.     	윤서현	        최초 생성
 *	
 * </pre>
 */
@Controller
@RequiredArgsConstructor
public class AuthorizationTempCreateController {

	private final AuthorizationTempService authService; 
	
	@PostMapping("/approval/tempSave")
	@ResponseBody
	public String tempSave(@ModelAttribute AuthorizationTempVO authTemp) {
	    // 1. 로그인한 사용자 ID 가져오기
	    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
	    String userId = auth.getName();

	    // 2. VO에 서버에서 세팅
	    authTemp.setAtrzUserId(userId);
	    authTemp.setAtrzSbmtDt(LocalDateTime.now()); // 제출일 현재 시각
	    authTemp.setDelYn("N");
	    authTemp.setOpenYn("N");

	    // 3. 서비스 호출
	    boolean success = authService.createAuthorizationTemp(authTemp);
	    return success ? "success" : "fail";
	}
	
}
