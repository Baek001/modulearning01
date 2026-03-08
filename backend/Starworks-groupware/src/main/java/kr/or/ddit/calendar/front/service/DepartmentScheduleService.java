package kr.or.ddit.calendar.front.service;

import java.util.List;

import kr.or.ddit.vo.DepartmentScheduleVO;

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
public interface DepartmentScheduleService {
	
	/**
	 * 부서 일정 목록 전체 조회. (페이징 X)
	 * @return 조회 결과 없으면 list.size() == 0
	 */
	public List<DepartmentScheduleVO> readDepartmentScheduleList();

	public List<DepartmentScheduleVO> readDepartmentScheduleList(String deptId);
	
	/**
	 * 부서 일정 단건 조회.
	 * @param deptSchdId : Department Schedule ID 
	 * @return 조회 결과 없으면 EntityNotFoundException
	 */
	public DepartmentScheduleVO readDepartmentSchedule(String deptSchdId);
	
	/**
	 * 부서 일정 추가.
	 * @param ds : DepartmentSchedule VO 객체
	 * @return 성공하면 true, 실패하면 false
	 */
	public boolean createDepartmentSchedule(DepartmentScheduleVO ds);
	
	/**
	 * 부서 일정 수정.
	 * @param ds : DepartmentSchedule VO 객체
	 * @return 성공하면 true, 실패하면 false
	 */
	public boolean modifyDepartmentSchedule(DepartmentScheduleVO ds);

	public boolean removeDepartmentSchedule(String deptSchdId);
}
