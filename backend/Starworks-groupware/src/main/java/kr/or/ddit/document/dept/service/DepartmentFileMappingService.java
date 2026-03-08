package kr.or.ddit.document.dept.service;

import java.util.List;

import kr.or.ddit.comm.paging.PaginationInfo;
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
 *    수정일      		수정자              수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 26.     	임가영	          최초 생성
 *  2025. 10. 17.		임가영			  부서문서함, 전사문서함 삭제된 파일 목록 가져오는 Mapper 추가
 * </pre>
 */
public interface DepartmentFileMappingService {

	/**
	 * 부서 문서 전체 목록 조회. (페이징 O)
	 * @param deptId 부서 일련번호
	 * @return 조회 결과 없으면 list.size() == 0
	 */
	public List<DepartmentFileMappingVO> readDepartmentFileMappingList(PaginationInfo<DepartmentFileMappingVO> paging, String deptId);

	/**
	 * 부서 문서 전체 목록 조회. (페이징 X)
	 * @param deptId 부서 일련번호
	 * @return 조회 결과 없으면 list.size() == 0
	 */
	public List<DepartmentFileMappingVO> readDepartmentFileMappingListNonPaging(String deptId);

	/**
	 * 부서 문서 등록
	 * @param departmentFileMapping 부서 Id, 파일 Id
	 * @return 성공하면 true, 실패하면 false
	 */
	public boolean createDepartmentFileMapping(DepartmentFileMappingVO departmentFileMapping);

	/**
	 * 부서&전사 문서함 삭제된 파일 전부 가져오기
	 * @param userId
	 * @return
	 */
	public List<DepartmentFileMappingVO> readDepartmentFileMappingVONonPagingByDelY();
}
