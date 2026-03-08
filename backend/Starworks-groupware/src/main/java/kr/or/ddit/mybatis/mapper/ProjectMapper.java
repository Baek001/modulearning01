package kr.or.ddit.mybatis.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import kr.or.ddit.comm.paging.PaginationInfo;
import kr.or.ddit.vo.ProjectVO;

/**
 *
 * @author 김주민
 * @since 2025. 9. 25.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 25.     	김주민	          최초 생성
 *  2025. 10. 01. 		김주민			  보관함 조회 추가 selectArchivedProjectList
 *  2025. 10. 10.		김주민			  프로젝트 진행률을 위한 select/update ProjectProgress 추가
 *  2025. 10. 11.		김주민			  프로젝트 완료 처리 completeProject
 *  2025. 10. 16. 		김주민			  진행한 프로젝트(완료상태) selectMyCompletedProjectList 조회 추가
 *  2025. 10. 21.		김주민			  관리자 페이지 취소 복원기능을 위한 updateProjectStatus 추가
 *
 * </pre>
 */
@Mapper
public interface ProjectMapper {
	int selectTotalRecord(PaginationInfo<ProjectVO> paging);
	int selectMyProjectTotalRecord(Map<String, Object> paramMap); //진행 중인 프로젝트
	int selectMyCompletedProjectTotalRecord(Map<String, Object> paramMap); // 진행한 프로젝트
	int selectArchivedTotalRecord(PaginationInfo<ProjectVO> paging); // 전사 중단 프로젝트

	/**
	 * 프로젝트 상태 업데이트
	 * @param project
	 * @return
	 */
	public int updateProjectStatus(ProjectVO project);

	/**
	 * React 프로젝트 목록 조회.
	 * @param userId 로그인한 사용자 ID
	 * @return 접근 가능한 프로젝트 목록
	 */
	public List<ProjectVO> selectProjectListForReact(String userId);

	/**
	 * React 프로젝트 상세 조회.
	 * @param bizId 프로젝트 ID
	 * @return 프로젝트 상세
	 */
	public ProjectVO selectProjectForReact(String bizId);

	/**
	 * React 프로젝트 상태 코드 변경.
	 * @param bizId 프로젝트 ID
	 * @param bizSttsCd 상태 코드
	 * @return 업데이트 건수
	 */
	public int updateProjectStatusCode(@Param("bizId") String bizId, @Param("bizSttsCd") String bizSttsCd);

	/**
	 * 내가 진행한 프로젝트 목록 조회(B304)
	 * @param paramMap
	 * @return
	 */
	public List<ProjectVO> selectMyCompletedProjectList(Map<String, Object> paramMap);

	/**
	 * 프로젝트 완료 처리
	 * @param bizId 프로젝트 ID
	 * @return 업데이트된 행의 수
	 */
	public int completeProject(String bizId);

	/**
     * 프로젝트 진행률 조회 (업무들의 평균 진행률)
     * @param bizId
     * @return 프로젝트 진행률 (0~100)
     */
    public int selectProjectProgress(String bizId);

    /**
     * 프로젝트 진행률 업데이트 (업무들의 평균 진행률로 자동 계산)
     * @param bizId
     * @return 업데이트된 행 수
     */
    public int updateProjectProgress(String bizId);

	/**
	 * 보관함 : 취소된 프로젝트 목록 조회 (페이징O)
	 * @param paging
	 * @return
	 */
	public List<ProjectVO> selectArchivedProjectList(PaginationInfo<ProjectVO> paging);

	/**
	 * 내 프로젝트 목록(현재는 '진행 중인 프로젝트'로 사용) 조회 (페이징O)
	 * @param paramMap
	 * @return
	 */
	public List<ProjectVO> selectMyProjectList(Map<String, Object> paramMap);

	/**
	 * 프로젝트 목록 조회 (페이징O)
	 * @param paging
	 * @return
	 */
	public List<ProjectVO> selectProjectList(PaginationInfo<ProjectVO> paging);

	/**
	 * 프로젝트 등록
	 * @param newProject
	 * @return
	 */
	public int insertProject(ProjectVO newProject);

	/**
	 * 프로젝트 목록 조회 (페이징X)
	 * @return
	 */
	public List<ProjectVO> selectProjectListNonPaging();

	/**
	 * 내가 참여하는 프로젝트 목록 조회 (페이징 X)
	 * @param userId 로그인한 사용자 ID
	 * @return
	 */
	public List<ProjectVO> selectMyProjectListNonPaging(String userId);

	/**
	 * 프로젝트 단건 조회
	 * @param bizId
	 * @return 조회 결과 없으면 null
	 */
	public ProjectVO selectProject(String bizId);

	/**
	 * 프로젝트 수정
	 * @param project
	 * @return
	 */
	public int updateProject(ProjectVO project);

	/**
	 * 프로젝트 취소(삭제)
	 * @param bizId
	 * @return
	 */
	public int deleteProject(String bizId);


}
