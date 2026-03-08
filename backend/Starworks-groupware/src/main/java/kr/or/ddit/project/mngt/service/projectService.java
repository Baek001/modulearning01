package kr.or.ddit.project.mngt.service;

import java.util.List;
import java.util.Map;

import kr.or.ddit.comm.paging.PaginationInfo;
import kr.or.ddit.vo.ProjectVO;

/**
 *
 * @author 김주민
 * @since 2025. 9. 25.
 * @see projectService
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 25.     	김주민	          최초 생성
 *  2025. 9. 29. 		김주민 			  readMyProjectList, readProjectList 추가
 *  2025. 10. 01.		김주민 			  convertProjectToTemplateData 추가
 *  2025. 10. 16. 		김주민			  내 프로젝트 서비스 분리 -> readMyCompletedProjectList 추가
 *  2025. 10. 21.		김주민			  restoreProject 추가
 *
 * </pre>
 */
public interface projectService {

	/**
	 * 프로젝트 복원 (취소된 프로젝트를 진행 중으로 변경)
	 * @param bizId 프로젝트 ID
	 * @return 성공 여부
	 */
	public boolean restoreProject(String bizId);

	/**
	 * '진행한 프로젝트' 목록 조회(B304)
	 * @param userId
	 * @param paging
	 * @return
	 */
	public List<ProjectVO> readMyCompletedProjectList(String userId, PaginationInfo<ProjectVO> paging);


	/**
	 * 프로젝트 완료 처리
	 * @param bizId
	 * @return 성공 여부에 따라 0으로 false
	 */
	public boolean completeProject(String bizId);

	/**
	 * 보관함 조회 (페이징O)
	 * @param paging
	 * @return 조회 결과 없으면 list.size() == 0
	 */
	public List<ProjectVO> readArchivedProjectList(PaginationInfo<ProjectVO> paging);

	/**
	 * 프로젝트 데이터를 결재 템플릿 용으로 변환
	 * @param bizId 프로젝트 ID
	 * @return 템플릿 플레이스홀더와 값의 Map
	 */
	public Map<String, String> convertProjectToTemplateData(String bizId);

	/**
	 * 내 프로젝트 목록 조회(페이징O)
	 * @param userId
	 * @param paging
	 * @return
	 */
	public List<ProjectVO> readMyProjectList(String userId, PaginationInfo<ProjectVO> paging);

	/**
	 * 프로젝트 목록 조회 (페이징O)
	 * @param paging
	 * @return 조회 결과 없으면 list.size() == 0
	 */
	public List<ProjectVO> readProjectList(PaginationInfo<ProjectVO> paging);

	/**
	 * 프로젝트 등록을 처리
	 * @param newProject
	 * @return 등록 성공 시 프로젝트 ID, 실패 시 null
	 */
	public String createProject(ProjectVO newProject);

	/**
	 * 프로젝트 목록 조회 (페이징X)
	 * @return
	 */
	public List<ProjectVO> readProjectListNonPaging();

	/**
	 * 내 프로젝트 목록 조회 (페이징X)
	 * @param userId
	 * @return
	 */
	public List<ProjectVO> readMyProjectListNonPaging(String userId);

	/**
	 * 프로젝트 단건 조회
	 * @param bizId
	 * @return 조회 결과 없으면 EntityNotFoundException
	 */
	public ProjectVO readProject(String bizId);

	/**
	 * 프로젝트 수정
	 * @param project
	 * @return 조회 결과가 없으면 0 으로 false
	 */
	public boolean modifyProject(ProjectVO project);

	/**
	 * 프로젝트 취소(삭제)
	 * @param bizId
	 * @return 조회 결과가 없으면 0 으로 false
	 */
	public boolean removeProject(String bizId);

}
