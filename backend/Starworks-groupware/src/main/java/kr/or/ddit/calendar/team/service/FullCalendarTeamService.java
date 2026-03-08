package kr.or.ddit.calendar.team.service;

import java.util.List;

import kr.or.ddit.dto.FullCalendarTeamDTO;

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
 *  2025.10. 16.     	장어진	          파라미터 수정
 *
 * </pre>
 */
public interface FullCalendarTeamService {

	/**
	 * FullCalendar Team 전체 조회.
	 * @param userId
	 * @return 조회 결과 없으면 list.size() == 0
	 */
	public List<FullCalendarTeamDTO> readFullCalendarTeamList(String userId);
}
