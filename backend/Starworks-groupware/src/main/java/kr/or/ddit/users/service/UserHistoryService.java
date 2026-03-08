package kr.or.ddit.users.service;

import java.util.List;

import kr.or.ddit.vo.UserHistoryVO;

/**
 *
 * @author 임가영
 * @since 2025. 10. 15.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 15.     	임가영           최초 생성
 *
 * </pre>
 */
public interface UserHistoryService {

	/**
	 * 인사기록 전체 조회
	 * @return 조회 결과 없으면 list().size == 0
	 */
	public List<UserHistoryVO> readUserHistoryList();
	/**
	 * 부서별 인사기록 조회
	 * @param deptId 부서 Id
	 * @return 조회 결과 없으면 list().size == 0
	 */
	public List<UserHistoryVO> readUserHistoryByDept(String deptId);
	/**
	 * 개인별 인사기록 조회
	 * @param userId 사용자 Id
	 * @return 조회 결과 없으면 list().size == 0
	 */
	public List<UserHistoryVO> readUserHistoryByUser(String userId);
	/**
	 * 인사기록 한 건 조회
	 * @param historyId 인사기록 Id
	 * @return 조회 결과 없으면 EntityNotFoundException 발생
	 */
	public UserHistoryVO readUserHistory(Integer historyId);
	/**
	 * 인사기록 데이터 삽입
	 * @param userHistory 대상직원, 변경전부서, 변경후부서, 변경전직급코드, 변경후직급코드, 변경일을 담은 VO
	 * @return 성공하면 true, 실패하면 false
	 */
	public boolean createUserHistory(UserHistoryVO userHistory);

}
