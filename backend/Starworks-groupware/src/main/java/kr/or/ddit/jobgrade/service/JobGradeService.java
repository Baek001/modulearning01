package kr.or.ddit.jobgrade.service;

import java.util.List;

import kr.or.ddit.vo.JobGradeVO;

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
 *
 * </pre>
 */
public interface JobGradeService {
	/**
	 * 조직도 직급 목록 조회
	 * @return
	 */
	public List<JobGradeVO> readJobGradeList();
	/**
	 * 조직도 직급 상세 조회
	 * @param jbgdCd
	 * @return
	 */
	public JobGradeVO readJobGrade(String jbgdCd);
}
