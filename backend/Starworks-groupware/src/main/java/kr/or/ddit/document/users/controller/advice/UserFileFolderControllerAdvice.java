package kr.or.ddit.document.users.controller.advice;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import kr.or.ddit.document.users.service.DocumentUserFileFolderService;
import kr.or.ddit.vo.UserFileFolderVO;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author 임가영
 * @since 2025. 10. 15.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           	  수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 15.     	임가영	          최초 생성
 *
 * </pre>
 */
@ControllerAdvice(basePackages = "kr.or.ddit.document.users.controller")
@RequiredArgsConstructor
public class UserFileFolderControllerAdvice {

	private final DocumentUserFileFolderService service;

	@ModelAttribute("allFolderList")
	public List<UserFileFolderVO> getAllFolderList(Authentication authentication) {
		String username = authentication.getName();
		List<UserFileFolderVO> allFolderList = service.retrieveAllFolderList(username);

		return allFolderList;
	}
}
