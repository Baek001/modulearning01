package kr.or.ddit.mybatis.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import kr.or.ddit.vo.DepartmentVO;

/**
 *
 * @author 윤서현
 * @since 2025. 10. 27.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 27.     	윤서현	          최초 생성
 *
 * </pre>
 */
@Mapper
public interface DepartmentMapper {

	/**
	 * 부서 등록
	 * @param department
	 * @return
	 */
	public int insertDepartment(DepartmentVO department);

	/**
	 * 부서 리스트
	 * @return
	 */
	public List<DepartmentVO> selectDepartmentList();

	/**
	 * 부서 상세정보
	 * @param deptId
	 * @return
	 */
	public DepartmentVO selectDepartment(String deptId);

	/**
	 *
	 * @param deptId
	 * @return
	 */
	public int countUsersInDepartment(String deptId);

	/**
	 * 부서 삭제
	 * @param deptId
	 * @return
	 */
	public int deleteDepartment(String deptId);

    /**
     * 부서별 사용자 수를 조회합니다.
     * @return 부서명과 사용자 수를 포함하는 맵 리스트
     */
    List<Map<String, Object>> selectDepartmentUserCounts();


    //트리구조에 필요함
    /**
     * 상위부서 없는 경우
     * @return
     */
    public String getNextTopDeptId(); //상위부서 없는 경우

    /**
     * 상위부서 있는 경우
     * @param upDeptId
     * @return
     */
    public String getNextChildDeptId(String upDeptId); //상위부서 있는 경우

    /**
     * 부서 수정
     * @param deptId
     * @return
     */
    public int updateDepartment(DepartmentVO dept);
}
