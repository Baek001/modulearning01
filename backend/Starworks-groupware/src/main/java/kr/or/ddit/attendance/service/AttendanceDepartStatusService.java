package kr.or.ddit.attendance.service;

import java.util.List;

import kr.or.ddit.dto.AttendanceDepartStatusDTO;

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
public interface AttendanceDepartStatusService {

	/**
	 * 부서 근태 현황 조회
	 * @param dto
	 * @return
	 */
	public List<AttendanceDepartStatusDTO> readAttendanceDepartStatusList(AttendanceDepartStatusDTO dto);

	/**
	 * 부서 근태 현황 조회 달력용
	 * @param dto
	 * @return
	 */
	public List<AttendanceDepartStatusDTO> readAttendanceDepartStatisticsList(AttendanceDepartStatusDTO dto);
}
