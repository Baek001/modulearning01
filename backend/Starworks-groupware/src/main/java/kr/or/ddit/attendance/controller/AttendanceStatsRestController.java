package kr.or.ddit.attendance.controller;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kr.or.ddit.attendance.service.AttendanceDepartStatusService;
import kr.or.ddit.attendance.service.UserMonthlyAttendanceService;
import kr.or.ddit.attendance.service.UserWeeklyAttendanceService;
import kr.or.ddit.dto.AttendanceDepartStatusDTO;
import kr.or.ddit.dto.UserMonthlyAttendanceDTO;
import kr.or.ddit.dto.UserWeeklyAttendanceDTO;
import kr.or.ddit.security.CustomUserDetails;
import kr.or.ddit.vo.UsersVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author 장어진
 * @since 2025. 10. 15.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 15.     	장어진	          최초 생성
 *
 * </pre>
 */
@Slf4j
@RestController
@RequestMapping("/rest/attendance-stats")
@RequiredArgsConstructor
public class AttendanceStatsRestController {

	private final AttendanceDepartStatusService adsService;
	private final UserMonthlyAttendanceService umaService;
	private final UserWeeklyAttendanceService uwaService;

	/**
	 * 부서원 월별 근태 현황
	 * @param authentication
	 * @return
	 */
	@GetMapping("/month")
	public Map<String, Object> getMonthAttendance(
		Authentication authentication
	){
		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
		UsersVO realUser = userDetails.getRealUser();
		LocalDate now = LocalDate.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");

		log.error("formatted date : {}", now.format(formatter));

		UserMonthlyAttendanceDTO dto = new UserMonthlyAttendanceDTO();
		dto.setUserId(realUser.getUserId());
		dto.setWorkMonth(now.format(formatter));

		UserMonthlyAttendanceDTO umaDTO = umaService.readUserMonthlyAttendance(dto);

		Map<String, Object> result = new HashMap<>();
		result.put("umaDTO", umaDTO);
		return result;
	}

	/**
	 * 부서 월별 근태 현황
	 * @param authentication
	 * @return
	 */
	@GetMapping("/month-list")
	public Map<String, Object> getMonthListAttendance(
		Authentication authentication
	){
		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
		UsersVO realUser = userDetails.getRealUser();
		LocalDate now = LocalDate.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");

		UserMonthlyAttendanceDTO dto = new UserMonthlyAttendanceDTO();
		dto.setDeptId(realUser.getDeptId());
		dto.setWorkMonth(now.format(formatter));

		List<UserMonthlyAttendanceDTO> umaDTOLA = umaService.readUserMonthlyAttendanceLateAbsent(dto);

		Map<String, Object> result = new HashMap<>();
		result.put("umaDTOLA", umaDTOLA);
		return result;
	}

	/**
	 * 부서원 주별 근태 현황
	 * @param authentication
	 * @return
	 */
	@GetMapping("/week")
	public Map<String, Object> getWeekAttendance(
		Authentication authentication
	){
		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
		UsersVO realUser = userDetails.getRealUser();
		LocalDate now = LocalDate.now();
		LocalDate startOfWeek = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

		UserWeeklyAttendanceDTO dto = new UserWeeklyAttendanceDTO();
		dto.setUserId(realUser.getUserId());
		dto.setWorkWeekStartDate(startOfWeek);

		UserWeeklyAttendanceDTO uwaDTO = uwaService.readUserWeeklyAttendance(dto);

		Map<String, Object> result = new HashMap<>();
		result.put("uwaDTO", uwaDTO);

		return result;
	}

	/**
	 * 부서별 주별 근태 현황 조회
	 * @param authentication
	 * @return
	 */
	@GetMapping("/depart")
	public Map<String, Object> getDepartAttendance(
		Authentication authentication
	){
		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
		UsersVO realUser = userDetails.getRealUser();
		LocalDate now = LocalDate.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

		AttendanceDepartStatusDTO dto = new AttendanceDepartStatusDTO();
		dto.setDeptId(realUser.getDeptId());
		dto.setWorkYmd(now.format(formatter));

		List<AttendanceDepartStatusDTO> adsDTOList = adsService.readAttendanceDepartStatusList(dto);
		List<AttendanceDepartStatusDTO> adsDTOStatsList = adsService.readAttendanceDepartStatisticsList(dto);

		Map<String, Object> result = new HashMap<>();
		result.put("adsDTOList", adsDTOList);
		result.put("adsDTOStatsList", adsDTOStatsList);
		return result;
	}
}
