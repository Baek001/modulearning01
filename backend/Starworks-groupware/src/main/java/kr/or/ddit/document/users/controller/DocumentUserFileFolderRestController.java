package kr.or.ddit.document.users.controller;

import java.util.HashMap;
import java.util.Map;

import org.apache.ibatis.annotations.Delete;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kr.or.ddit.document.users.service.DocumentUserFileFolderService;
import kr.or.ddit.document.users.service.DocumentUserFileService;
import kr.or.ddit.security.CustomUserDetails;
import kr.or.ddit.vo.UserFileFolderVO;
import kr.or.ddit.vo.UsersVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author 장어진
 * @since 2025. 10. 10.
 * @see
 *
 *      <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 10.     	장어진	          최초 생성
 *
 *      </pre>
 */
@Slf4j
@RestController
@RequestMapping("/rest/document/user/folder")
@RequiredArgsConstructor
public class DocumentUserFileFolderRestController {
	private final DocumentUserFileFolderService service;
	private final DocumentUserFileService ufmService;

	@PostMapping
	public Map<String, Object> createFolder(@ModelAttribute UserFileFolderVO userFileFolder,
			Authentication authentication) {

		String username = authentication.getName();
		userFileFolder.setUserId(username);

		String trimFolderNm = userFileFolder.getFolderNm().trim();
		userFileFolder.setFolderNm(trimFolderNm);

		boolean success = service.createFolder(userFileFolder);

		Map<String, Object> result = new HashMap<>();
		result.put("success", success);
		result.put("userFileFolder", userFileFolder);
		return result;
	}

//	@PutMapping("/rename")
//	public Map<String, Object> renameFolder(
//		@RequestBody UserFileFolderVO uffVO
//		, Authentication authentication
//	) {
//		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
//		UsersVO realUser = userDetails.getRealUser();
//		uffVO.setUserId(realUser.getUserId());
//
//		boolean success = service.modifyFolderName(uffVO);
//
//		Map<String, Object> result = new HashMap<>();
//		result.put("success", success);
//		return result;
//	}
//
	/**
	 * 폴더 이동
	 * @param uffVO 해당 파일의 폴더Id 와 이동하려는 폴더Id 정보를 담은 UserFileFolderVO
	 * @param authentication
	 * @return
	 */
	@PutMapping("/move")
	public Map<String, Object> moveFolder(
		@RequestBody UserFileFolderVO uffVO
	) {
		boolean success = service.moveFolder(uffVO);

		Map<String, Object> result = new HashMap<>();
		result.put("success", success);
		return result;
	}

	/**
	 * 폴더 삭제
	 * @param uffVO
	 * @param authentication
	 * @return
	 */
//	@DeleteMapping
//	public Map<String, Object> removeFolder(
//		@RequestBody UserFileFolderVO uffVO
//	) {
//
//		boolean success = service.removeFolder(uffVO.getFolderSqn());
//
//		Map<String, Object> result = new HashMap<>();
//		result.put("success", success);
//		return result;
//	}
}
