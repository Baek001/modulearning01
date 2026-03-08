package kr.or.ddit.vacation.service;

import java.util.List;

import kr.or.ddit.vo.VactionVO;

/**
 *
 * @author 윤서현
 * @since 2025. 9. 25.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 25.     	윤서현	          최초 생성
 *  2025. 10. 23.     	장어진            연차 계산 기능 추가
 *
 * </pre>
 */
public interface VactionService {

	/**
	 * 전자결재 휴가원 일정 캘린더 등록
	 * @param vac
	 * @return
	 */
	public boolean createVaction(VactionVO vac);
	/**
	 * 전자결재 휴가원일정 캘린더 목록 조회
	 * @return
	 */
	public List<VactionVO> readVactionList();
	/**
	 * 전자결재 휴가원일정 캘린더 목록 상세조회
	 * @param vactSqn
	 * @return
	 */
	public VactionVO readVaction(String vactSqn);

	/**
	 * 전자결재 휴가일정 사용자 연차 계산용 조회
	 * @param vac
	 * @return 없으면 0
	 */
	public int readVacationDaysByUser(String vactUserId, String vactCd, Integer year);
}
