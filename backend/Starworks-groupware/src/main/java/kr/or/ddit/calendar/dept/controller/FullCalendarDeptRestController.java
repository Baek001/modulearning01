package kr.or.ddit.calendar.dept.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kr.or.ddit.calendar.dept.service.FullCalendarDeptService;
import kr.or.ddit.dto.FullCalendarDeptDTO;
import kr.or.ddit.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;

/**
 * 
 * @author 장어진
 * @since 2025. 9. 27.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 27.     	장어진	          최초 생성
 *
 * </pre>
 */
@RestController
@RequestMapping("/rest/fullcalendar-dept")
@RequiredArgsConstructor
public class FullCalendarDeptRestController {
	
	private final FullCalendarDeptService service;
	private FullCalendarDeptDTO dto;
	
	/**
	 * FullCalendar 부서 일정 목록을 조회 
	 * @param authentication 인증된 사용자의 userId와 DeptId를 꺼내옴
	 * @return null일 시 list.size() == 0
	 */
	@GetMapping
	public List<FullCalendarDeptDTO> readFullCalendarDeptList(
		Authentication authentication
	){
		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
		dto = new FullCalendarDeptDTO();
		dto.setUserId(userDetails.getRealUser().getUserId());
		dto.setDeptId(userDetails.getRealUser().getDeptId());
		return service.readFullCalendarDeptList(dto);
	}
}
