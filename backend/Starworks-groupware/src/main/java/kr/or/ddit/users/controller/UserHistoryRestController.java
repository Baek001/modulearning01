package kr.or.ddit.users.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;


import kr.or.ddit.users.service.UserHistoryService;
import kr.or.ddit.vo.UserHistoryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author 임가영
 * @since 2025. 10. 15.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 15.     	임가영           최초 생성
 *
 * </pre>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping
@Slf4j
public class UserHistoryRestController {

	private final UserHistoryService service;

	/**
	 * 인사기록 전체 조회
	 */
	@GetMapping({"/rest/comm-user-history", "/rest/comm-user-history/{filter}"})
	public List<UserHistoryVO> readUserHistoryList(
		@PathVariable(required = false) String filter,
		@RequestParam(required = false, name = "deptId") String deptId,
		@RequestParam(required = false, name = "userId") String userId) {

		List<UserHistoryVO> userHistoryList = new ArrayList<>();

		if (StringUtils.isBlank(filter)) {
			// 인사기록 전체 조회 ("/rest/comm-user-history")
			userHistoryList = service.readUserHistoryList();

		} else if (filter.equals("dept")) {
			// 부서별 인사기록 조회 ("/rest/comm-user-history/dept?deptId=dept")
			userHistoryList = service.readUserHistoryByDept(deptId);

		} else if (filter.equals("user")) {
			// 개인별 인사기록 조회 ("/rest/comm-user-history/user?filter=userId")
			userHistoryList = service.readUserHistoryByUser(userId);

		} else {
			// 잘못된 쿼리스트링
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 filter 값");
		}

		return userHistoryList;
	}

	/**
	 * 인사기록 한 건 조회
	 */
	@GetMapping("/rest/comm-user-history/{historyId}")
	public UserHistoryVO readUserHistory(@PathVariable Integer historyId) {
		return service.readUserHistory(historyId);
	}

	/**
	 * 인사기록 데이터 삽입
	 * (필요시 @ReqeustBody => @ModelAttribute 변경 가능)
	 */
	@PostMapping
	public Map<String, Object> createUserHistory(@RequestBody UserHistoryVO userHistory) {
		boolean success = service.createUserHistory(userHistory);

		Map<String, Object> respMap = new HashMap<>();
		respMap.put("success", success);

		return respMap;
	}
}
