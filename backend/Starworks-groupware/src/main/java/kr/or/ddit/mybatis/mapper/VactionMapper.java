package kr.or.ddit.mybatis.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

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
 *  2025. 10. 23.     	장어진            연차 계산 SQL문 추가
 *
 * </pre>
 */
@Mapper
public interface VactionMapper {

	/**
	 * 전자결재 휴가원 일정 캘린더 등록
	 * @param vac
	 * @return
	 */
	public int insertVaction(VactionVO vac);
	/**
	 * 전자결재 휴가원일정 캘린더 목록 조회
	 * @return
	 */
	public List<VactionVO> selectVactionList();
	/**
	 * 전자결재 휴가원일정 캘린더 목록 상세조회
	 * @param vactSqn
	 * @return
	 */
	public VactionVO selectVacation(String vactSqn);

	/**
	 * 전자결재 휴가일정 사용자 연차 계산용 조회
	 * @param
	 * @return
	 */
	public int selectVacationDaysbyUser(String vactUserId, String vactCd, Integer year);
}
