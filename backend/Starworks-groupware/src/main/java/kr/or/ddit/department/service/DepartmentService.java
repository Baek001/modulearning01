package kr.or.ddit.department.service;

import java.util.List;

import kr.or.ddit.vo.DepartmentVO;

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
public interface DepartmentService {

	/**
	 * 조직도 부서 추가
	 * @param dept
	 * @return
	 */
	public boolean createDepartment(DepartmentVO dept);
	/**
	 * 조직도 부서 목록 조회
	 * @return
	 */
	public List<DepartmentVO> readDepartmentList();
	/**
	 * 조직도 부서 상세 조회
	 * @param deptId
	 * @return
	 */
	public DepartmentVO readDepartment(String deptId);
	/**
	 * 조직도 부서 삭제
	 * @param deptId
	 * @return
	 */
	public boolean removeDepartment(String deptId);

	/**
	 * 부서 수정
	 * @param deptId
	 * @return
	 */
	public boolean modifyDepartment(DepartmentVO dept);

}
