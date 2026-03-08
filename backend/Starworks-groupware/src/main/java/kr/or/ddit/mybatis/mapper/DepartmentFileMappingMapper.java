package kr.or.ddit.mybatis.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import kr.or.ddit.vo.DepartmentFileMappingVO;
import kr.or.ddit.vo.UserFileMappingVO;

/**
 *
 * @author 임가영
 * @since 2025. 9. 26.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자              수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 26.     	임가영	          최초 생성
 *  2025. 10. 17.		임가영			  부서문서함, 전사문서함 삭제된 파일 목록 가져오는 Mapper 추가
 *
 * </pre>
 */
@Mapper
public interface DepartmentFileMappingMapper {

	/**
	 * 페이징을 위한 전체 목록 수
	 * @param paging
	 * @return
	 */
	public int selectDepartmentFileMappingTotalRecoard(Map<String, Object> paramMap);

	/**
	 * 부서 문서 전체 목록 조회. (페이징 X)
	 * @param deptId 부서 일련번호
	 * @return 조회 결과 없으면 list.size() == 0
	 */
	public List<DepartmentFileMappingVO> selectDepartmentFileMappingListNonPaging(String deptId);

	/**
	 * 부서 문서 전체 목록 조회. (페이징 O)
	 * @param deptId
	 * @return 조회 결과 없으면 list.size() == 0
	 */
	public List<DepartmentFileMappingVO> selectDepartmentFileMappingList(Map<String, Object> paramMap);
	
	/**
	 * 전사 문서 목록 조회. (페이징 O)
	 * @param deptId
	 * @return 조회 결과 없으면 list.size() == 0
	 */
	public List<DepartmentFileMappingVO> selectCompanyFileMappingList(Map<String, Object> paramMap);

	/**
	 * 부서 문서 등록
	 * @param departmentFileMapping 부서 Id, 파일 Id
	 * @return 성공한 레코드 수
	 */
	public int insertDepartmentFileMapping(DepartmentFileMappingVO departmentFileMapping);

	/**
	 * 부서&전사 문서함 삭제된 파일 전부 가져오기
	 * @param ufmVO 유저 Id 가 담긴 vo
	 * @return
	 */
	public List<DepartmentFileMappingVO> selectDepartmentFileMappingVONonPagingByDelY(String userId);

}
