package kr.or.ddit.calendar.users.service;

import java.util.List;

import kr.or.ddit.vo.UserScheduleVO;

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
public interface UserScheduleService {
	
	/**
	 * 개인 일정 목록 조회 (페이징 X)
	 * @return 조회 결과 없으면 list.size() == 0
	 */
	public List<UserScheduleVO> readUserScheduleList();

	public List<UserScheduleVO> readUserScheduleList(String userId);
	
	/**
	 * 개인 일정 단건 조회.
	 * @param userSchdId : 사용자 일정 ID
	 * @return 조회 결과 없으면 EntityNotFoundException
	 */
	public UserScheduleVO readUserSchedule(String userSchdId);
	
	/**
	 * 개인 일정 생성.
	 * @param us : User Schedule VO 객체
	 * @return 성공하면 true, 실패하면 false
	 */
	public boolean createUserSchedule(UserScheduleVO us);
	
	/**
	 * 개인 일정 수정.
	 * @param us : User Schedule VO 객체
	 * @return 성공하면 true, 실패하면 false
	 */
	public boolean modifyUserSchedule(UserScheduleVO us);

	public boolean removeUserSchedule(String userSchdId);
}
