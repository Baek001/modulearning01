package kr.or.ddit.document.users.controller;

import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import kr.or.ddit.document.users.service.DocumentUserFileService;
import kr.or.ddit.security.CustomUserDetails;
import kr.or.ddit.vo.UserFileMappingVO;
import kr.or.ddit.vo.UsersVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author 장어진
 * @since 2025. 10. 10.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 10.     	장어진	          최초 생성
 *
 * </pre>
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class DocumentUserFileCreateController {
	public final DocumentUserFileService service;
	
	/**
	 * 파일 등록
	 * @param userFile
	 * @param authentication
	 * @return
	 */
	@PostMapping("/document/upload/user")
	public String uploadUserFile(
		@ModelAttribute UserFileMappingVO userFile
		, BindingResult errors
		, RedirectAttributes redirectAttributes
		, Authentication authentication
	) {
		
		String username = authentication.getName();
		userFile.setUserId(username);
		
		if(!errors.hasErrors()) {
			int cnt = service.createUserFileMapping(userFile);
			redirectAttributes.addFlashAttribute("icon", "success");
			redirectAttributes.addFlashAttribute("toastMessage",  "파일 등록에 성공했습니다!");
			return "redirect:/document/user";
		} else {
			redirectAttributes.addFlashAttribute("userFile", userFile);
			String errorName = BindingResult.MODEL_KEY_PREFIX + "userFile";
			redirectAttributes.addFlashAttribute(errorName, errors);
			redirectAttributes.addFlashAttribute("toastMessage", "파일 등록에 실패했습니다");
			redirectAttributes.addFlashAttribute("icon", "error");
			return "redirect:/document/user";
		}
	}
}
