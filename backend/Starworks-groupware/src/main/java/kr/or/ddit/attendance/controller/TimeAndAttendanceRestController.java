package kr.or.ddit.attendance.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import kr.or.ddit.attendance.service.TimeAndAttendanceService;
import kr.or.ddit.security.CustomUserDetails;
import kr.or.ddit.vo.TimeAndAttendanceVO;
import kr.or.ddit.vo.UsersVO;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author 장어진
 * @since 2025. 9. 26.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 26.     	장어진	          최초 생성
 *  2025. 10. 15.		임가영			혹시 몰라서 쿼리스트링으로 workYmd 를 받는 readTimeAndAttendance 생성
 * </pre>
 */
@RestController
@RequestMapping("/rest/attendance")
@RequiredArgsConstructor
public class TimeAndAttendanceRestController {

	private final TimeAndAttendanceService service;

//	@GetMapping
//	public List<TimeAndAttendanceVO> readTimeAndAttendanceList(){
//		return service.readTimeAndAttendanceList();
//	}

	@GetMapping("/{userId}/{workYmd}")
	public Map<String, Object> readTimeAndAttendance(
		@PathVariable("userId") String userId
		, @PathVariable("workYmd") String workYmd
	) {
		TimeAndAttendanceVO taaVO = service.readTimeAndAttendance(userId, workYmd);

		Map<String, Object> result = new HashMap<>();
		result.put("taaVO", taaVO);
		result.put("now", LocalDateTime.now());

		return result;
	}

	@GetMapping("/{userId}/today")
	public Map<String, Object> readTimeAndAttendanceQueryString(
		@PathVariable("userId") String userId
	) {

		String workYmd = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
		TimeAndAttendanceVO taaVO = service.readTimeAndAttendance(userId, workYmd);

		Map<String, Object> result = new HashMap<>();
		result.put("taaVO", taaVO);
		result.put("now", LocalDateTime.now());

		return result;
	}

	@GetMapping("/{userId}")
	public Map<String, Object> readUserTimeAndAttendance(
			@PathVariable("userId") String userId
		) {
			List<TimeAndAttendanceVO> listTAA = service.readUserTimeAndAttendanceList(userId);

			Map<String, Object> result = new HashMap<>();
			result.put("listTAA", listTAA);
			result.put("now", LocalDateTime.now());

			return result;
		}

	@PostMapping
	public Map<String, Object> createTimeAndAttendance(
		Authentication authentication
	){
		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
		UsersVO realUser = userDetails.getRealUser();

		boolean success = service.createTimeAndAttendance(realUser.getUserId());
		Map<String, Object> result = new HashMap<>();
		result.put("success", success);
		return result;
	}

	@PutMapping
	public Map<String, Object> modifyTimeAndAttendance(
		@RequestBody TimeAndAttendanceVO taaVO
		, Authentication authentication
	){
		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
		UsersVO realUser = userDetails.getRealUser();

		taaVO.setUserId(realUser.getUserId());

		boolean success = service.modifyTimeAndAttendance(taaVO);
		Map<String, Object> result = new HashMap<>();
		result.put("success", success);
		return result;
	}
}
