package kr.or.ddit.calendar.front.controller;

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

import kr.or.ddit.calendar.front.service.DepartmentScheduleService;
import kr.or.ddit.security.CustomUserDetails;
import kr.or.ddit.vo.DepartmentScheduleVO;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/rest/calendar-depart")
@RequiredArgsConstructor
public class DepartmentScheduleRestController {

	private final DepartmentScheduleService service;

	@GetMapping
	public List<DepartmentScheduleVO> readDepartmentScheduleList(
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		return service.readDepartmentScheduleList(requireDeptId(userDetails));
	}

	@GetMapping("/{deptSchdId}")
	public DepartmentScheduleVO readDepartmentSchedule(
		@PathVariable String deptSchdId,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		DepartmentScheduleVO schedule = service.readDepartmentSchedule(deptSchdId);
		String deptId = requireDeptId(userDetails);
		if (!deptId.equals(schedule.getDeptId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "소속 부서 일정만 조회할 수 있습니다.");
		}
		return schedule;
	}

	@PostMapping
	public Map<String, Object> createDepartmentSchedule(
		@RequestBody DepartmentScheduleVO vo,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		vo.setDeptId(requireDeptId(userDetails));
		vo.setDeptSchdCrtUserId(requireUserId(userDetails));
		vo.setDelYn("N");
		boolean success = service.createDepartmentSchedule(vo);
		return Map.of(
			"success", success,
			"deptSchdId", vo.getDeptSchdId()
		);
	}

	@PutMapping("/{deptSchdId}")
	public Map<String, Object> modifyDepartmentSchedule(
		@PathVariable String deptSchdId,
		@RequestBody DepartmentScheduleVO vo,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		DepartmentScheduleVO existing = service.readDepartmentSchedule(deptSchdId);
		String userId = requireUserId(userDetails);
		if (!userId.equals(existing.getDeptSchdCrtUserId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "작성자만 부서 일정을 수정할 수 있습니다.");
		}

		vo.setDeptSchdId(deptSchdId);
		vo.setDeptId(existing.getDeptId());
		vo.setDeptSchdCrtUserId(existing.getDeptSchdCrtUserId());
		if (vo.getDelYn() == null) {
			vo.setDelYn("N");
		}

		boolean success = service.modifyDepartmentSchedule(vo);
		return Map.of("success", success);
	}

	@DeleteMapping("/{deptSchdId}")
	public Map<String, Object> removeDepartmentSchedule(
		@PathVariable String deptSchdId,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		DepartmentScheduleVO existing = service.readDepartmentSchedule(deptSchdId);
		String userId = requireUserId(userDetails);
		if (!userId.equals(existing.getDeptSchdCrtUserId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "작성자만 부서 일정을 삭제할 수 있습니다.");
		}

		boolean success = service.removeDepartmentSchedule(deptSchdId);
		return Map.of("success", success);
	}

	private String requireUserId(CustomUserDetails userDetails) {
		if (userDetails == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
		}
		return userDetails.getUsername();
	}

	private String requireDeptId(CustomUserDetails userDetails) {
		if (userDetails == null || userDetails.getRealUser() == null || userDetails.getRealUser().getDeptId() == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "부서 정보를 확인할 수 없습니다.");
		}
		return userDetails.getRealUser().getDeptId();
	}
}
