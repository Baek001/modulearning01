package kr.or.ddit.calendar.users.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import kr.or.ddit.calendar.users.service.UserScheduleService;
import kr.or.ddit.security.CustomUserDetails;
import kr.or.ddit.vo.UserScheduleVO;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/rest/calendar-user")
@RequiredArgsConstructor
public class UserScheduleRestController {

	private final UserScheduleService service;

	@GetMapping
	public List<UserScheduleVO> readUserScheduleList(
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		return service.readUserScheduleList(requireUserId(userDetails));
	}

	@GetMapping("/{userSchdId}")
	public UserScheduleVO readUserSchedule(
		@PathVariable String userSchdId,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		UserScheduleVO schedule = service.readUserSchedule(userSchdId);
		String userId = requireUserId(userDetails);
		if (!userId.equals(schedule.getUserId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 일정만 조회할 수 있습니다.");
		}
		return schedule;
	}

	@PostMapping
	public Map<String, Object> createUserSchedule(
		@RequestBody UserScheduleVO vo,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		vo.setUserId(requireUserId(userDetails));
		vo.setDelYn("N");
		boolean success = service.createUserSchedule(vo);
		return Map.of(
			"success", success,
			"userSchdId", vo.getUserSchdId()
		);
	}

	@PutMapping("/{userSchdId}")
	public Map<String, Object> modifyUserSchedule(
		@PathVariable String userSchdId,
		@RequestBody UserScheduleVO vo,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		UserScheduleVO existing = service.readUserSchedule(userSchdId);
		String userId = requireUserId(userDetails);
		if (!userId.equals(existing.getUserId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 일정만 수정할 수 있습니다.");
		}

		vo.setUserSchdId(userSchdId);
		vo.setUserId(userId);
		if (vo.getDelYn() == null) {
			vo.setDelYn("N");
		}

		boolean success = service.modifyUserSchedule(vo);
		return Map.of("success", success);
	}

	@DeleteMapping("/{userSchdId}")
	public Map<String, Object> removeUserSchedule(
		@PathVariable String userSchdId,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		UserScheduleVO existing = service.readUserSchedule(userSchdId);
		String userId = requireUserId(userDetails);
		if (!userId.equals(existing.getUserId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 일정만 삭제할 수 있습니다.");
		}

		boolean success = service.removeUserSchedule(userSchdId);
		return Map.of("success", success);
	}

	private String requireUserId(CustomUserDetails userDetails) {
		if (userDetails == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
		}
		return userDetails.getUsername();
	}
}
