package kr.or.ddit.attendance.service;

import java.util.List;

import kr.or.ddit.vo.TimeAndAttendanceVO;

/**
 *
 * @author 장어진
 * @since 2025. 9. 25.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 25.     	장어진	          최초 생성
 *  2025. 9. 26.     	장어진	          주석 내용 추가
 *
 * </pre>
 */
public interface TimeAndAttendanceService {
	/**
	 * 근무 현황 목록 조회. (페이징 X)
	 * @return 조회 결과 없으면 list.size() == 0
	 */
	public List<TimeAndAttendanceVO> readTimeAndAttendanceList();

	/**
	 * 사용자 근무 현황 조회 (페이징 X)
	 * @param userId : 사용자 ID
	 * @return 조회 결과 없으면 list.size() == 0
	 */
	public List<TimeAndAttendanceVO> readUserTimeAndAttendanceList(String userId);

	/**
	 * 근무 현황 단건 조회
	 * @param userId : 사용자 ID
	 * @param workYmd : 근무 일자
	 * @return 조회 결과 없으면 EntityNotFoundException
	 */
	public TimeAndAttendanceVO readTimeAndAttendance(String userId, String workYmd);

	/**
	 * 근무 현황 추가.
	 * @param taaVO : TimeAndAttendance VO 객체
	 * @return 성공 시 true, 실패 시 false
	 */
	public boolean createTimeAndAttendance(String userId);

	/**
	 * 근무 현황 수정.
	 * @param taaVO : TimeAndAttendance VO 객체
	 * @return 성공 시 true, 실패 시 false
	 */
	public boolean modifyTimeAndAttendance(TimeAndAttendanceVO taaVO);
}
