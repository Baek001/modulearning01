package kr.or.ddit.attendance.service;

import java.util.List;

import kr.or.ddit.dto.UserMonthlyAttendanceDTO;

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
public interface UserMonthlyAttendanceService {

	/**
	 * 부서 월별 근태 (지각 및 결근) 현황
	 * @param dto
	 * @return
	 */
	public List<UserMonthlyAttendanceDTO> readUserMonthlyAttendanceLateAbsent(UserMonthlyAttendanceDTO dto);

	/**
	 * 부서원 월별 근태 현황
	 * @return
	 */
	public UserMonthlyAttendanceDTO readUserMonthlyAttendance(UserMonthlyAttendanceDTO dto);

}
