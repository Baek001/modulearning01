package kr.or.ddit.project.member.service;

import java.util.List;

import kr.or.ddit.vo.ProjectMemberVO;

/**
 *
 * @author 김주민
 * @since 2025. 9. 25.
 * @see projectMemberService
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 25.     	김주민	          최초 생성
 *  2025.10. 17.     	장어진	          진행중인 프로젝트 목록만 가져오는 메소드 추가
 *
 * </pre>
 */
public interface projectMemberService {

	/**
	 * 프로젝트 인원 추가
	 * @param newProjectMember
	 * @return 조회 결과가 없으면 0 으로 false
	 */
	public boolean createProjectMember(ProjectMemberVO newProjectMember);


	/**
	 * 개별 프로젝트 인원 목록 조회
	 * @param bizId
	 * @return 조회 결과 없으면 list.size() == 0
	 */
	public List<ProjectMemberVO> readProjectMemberList(String bizId);

	/**
	 * 개별 인원 참여 프로젝트 목록 조회
	 * @param bizUserId
	 * @return 조회 결과 없으면 list.size() == 0
	 */
	public List<ProjectMemberVO> readProjectList(String bizUserId);

	/**
	 * 개별 인원 참여 프로젝트 목록 조회
	 * @param bizUserId
	 * @return 조회 결과 없으면 list.size() == 0
	 */
	public List<ProjectMemberVO> readProjectListOnlyB302(String bizUserId);

	/**
	 * 프로젝트 인원 단건 조회
	 * @param projectMember
	 * @return 조회 결과 없으면 EntityNotFoundException
	 */
	public ProjectMemberVO readProjectMember(ProjectMemberVO projectMember);

	/**
	 * 프로젝트 인원 수정
	 * @param projectMember
	 * @return 조회 결과가 없으면 0 으로 false
	 */
	public boolean modifyProjectMember(ProjectMemberVO projectMember);

	/**
	 * 프로젝트 인원 삭제
	 * @param projectMember
	 * @return 조회 결과가 없으면 0 으로 false
	 */
	public boolean removeProjectMember(ProjectMemberVO projectMember);

}
