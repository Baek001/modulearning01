package kr.or.ddit.attendance.service;

import kr.or.ddit.dto.UserWeeklyAttendanceDTO;

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
public interface UserWeeklyAttendanceService {

	/**
	 * 부서원 주별 근태 현황
	 * @return
	 */
	public UserWeeklyAttendanceDTO readUserWeeklyAttendance(UserWeeklyAttendanceDTO dto);
}
