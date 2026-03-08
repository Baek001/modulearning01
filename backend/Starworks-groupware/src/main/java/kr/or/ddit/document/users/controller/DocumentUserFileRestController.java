package kr.or.ddit.document.users.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kr.or.ddit.document.users.service.DocumentUserFileService;
import kr.or.ddit.vo.UserFileMappingVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author 장어진
 * @since 2025. 9. 26.
 * @see
 *
 *      <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 26.     	장어진	          최초 생성
 *  2025. 10. 15.		임가영			restController 부활시킴, postMapping 추가
 *      </pre>
 */
@RestController
@RequestMapping("/rest/document-user-file")
@RequiredArgsConstructor
@Slf4j
public class DocumentUserFileRestController {

	private final DocumentUserFileService service;

	/**
	 * 개인 파일 목록 조회
	 *
	 * @param userId
	 * @return
	 */
//	@GetMapping("/{userId}")
//	public List<UserFileMappingVO> readUserFileMappingList(@PathVariable("userId") String userId){
//		return service.readUserFileMappingList(userId);
//	}

	/**
	 * 개인 파일 단건 조회
	 *
	 * @param userId
	 * @param userFileId
	 * @return
	 */
//	@GetMapping("/{userId}/{userFileId}")
//	public UserFileMappingVO readUserFileMapping(@PathVariable String userId, @PathVariable String userFileId) {
//		return service.readUserFileMapping(userId, userFileId);
//	}

	/**
	 * 파일 등록
	 * @param userFile
	 * @param authentication
	 * @return
	 */
	@PostMapping
	public Map<String, Object> createUserFileMapping(
			@ModelAttribute UserFileMappingVO userFile
			, Authentication authentication
		) {

		String username = authentication.getName();
		userFile.setUserId(username);

		int cnt = service.createUserFileMapping(userFile);

		return Map.of("cnt", cnt);
	}

	@PutMapping("/move")
	public Map<String, Object> moveUserFile(@RequestBody UserFileMappingVO ufmVO) {
		log.info("===================> ufmVO : {}", ufmVO);


		boolean success = service.moveUserFile(ufmVO);

		Map<String, Object> result = new HashMap<>();
		result.put("success", success);
		return result;
	}
}
