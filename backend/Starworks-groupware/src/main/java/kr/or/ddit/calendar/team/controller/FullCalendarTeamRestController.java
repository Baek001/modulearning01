package kr.or.ddit.calendar.team.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kr.or.ddit.calendar.team.service.FullCalendarTeamService;
import kr.or.ddit.dto.FullCalendarTeamDTO;
import kr.or.ddit.project.member.service.projectMemberService;
import kr.or.ddit.security.CustomUserDetails;
import kr.or.ddit.vo.ProjectMemberVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author 장어진
 * @since 2025. 9. 30.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 30.     	장어진	          최초 생성
 *
 * </pre>
 */
@Slf4j
@RestController
@RequestMapping("/rest/fullcalendar-team")
@RequiredArgsConstructor
public class FullCalendarTeamRestController {

	private final FullCalendarTeamService service;

	private final projectMemberService pmService;

	/**
	 * FullCalendar 팀프로젝트 일정 목록을 조회
	 * @param authentication 인증된 사용자의 userId를 꺼내옴
	 * @return null일 시 list.size() == 0
	 */
	@GetMapping("/events")
	public List<FullCalendarTeamDTO> readFullCalendarTeamList(
		Authentication authentication
	){
		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
		String userId = userDetails.getRealUser().getUserId();

		return service.readFullCalendarTeamList(userId);
	}

	/**
	 * FullCalendar 사용자의 프로젝트 목록을 조회
	 * @param authentication
	 * @return
	 */
	@GetMapping("/project-list")
	public List<ProjectMemberVO> readProjectList(
		Authentication authentication
	){
		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
		String userId = userDetails.getRealUser().getUserId();
		return pmService.readProjectListOnlyB302(userId);
	}
}
