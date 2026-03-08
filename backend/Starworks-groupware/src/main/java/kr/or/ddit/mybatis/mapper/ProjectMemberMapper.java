package kr.or.ddit.mybatis.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import kr.or.ddit.vo.ProjectMemberVO;

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
 *  2025. 9. 30.     	장어진	          사용자 한명이 무슨 프로젝트에 참여했는지에 대한 목록 조회 추가
 *  2025. 10. 01.		김주민			  프로젝트 수정 시에 필요한 중복체크를 위한 countProjectMember 추가
 *  2025. 10. 08. 		김주민			  권한 코드 조회 getProjectAuthority 추가
 *  2025. 10. 17. 		장어진			  진행중인 프로젝트 목록만 가져오는 메소드 추가
 *
 * </pre>
 */
@Mapper
public interface ProjectMemberMapper {

	/**
	 * 프로젝트에서 사용자의 권한 코드 조회
	 * @param bizId
	 * @param bizUserId
	 * @return
	 */
	public String getProjectAuthority(@Param("bizId") String bizId, @Param("bizUserId") String bizUserId);

	/**
	 * 프로젝트에 해당 멤버가 존재하는지 조회
	 * (프로젝트 수정 시, 중복체크를 위한 메서드)
	 * @param projectMember
	 * @return 존재하면 1, 존재하지 않으면 0
	 */
	public int countProjectMember(ProjectMemberVO projectMember);

	/**
	 * 프로젝트 인원 추가
	 * @param newProjectMember
	 * @return
	 */
	public int insertProjectMember(ProjectMemberVO newProjectMember);

	/**
	 * 모든 프로젝트 인원 목록 조회
	 * @return
	 */
	public List<ProjectMemberVO> selectProjectMemberListNonPaging();

	/**
	 * 개별 프로젝트 인원 목록 조회
	 * @param bizId
	 * @return
	 */
	public List<ProjectMemberVO> selectProjectMemberByProject(String bizId);

	/**
	 * 개별 사용자 프로젝트 목록 조회
	 * @param bizUserId
	 * @return
	 */
	public List<ProjectMemberVO> selectProjectByProjectMember(String bizUserId);

	/**
	 * 개별 사용자 프로젝트 목록 중 진행 중인 프로젝트만
	 * @param bizUserId
	 * @return
	 */
	public List<ProjectMemberVO> selectProjectByProjectMemberOnlyB302(String bizUserId);

	/**
	 * 프로젝트 인원 단건 조회 - 복합키 사용
	 * @param projectMember (bizId와 bizUserId 포함)
	 * @return
	 */
	public ProjectMemberVO selectProjectMember(ProjectMemberVO projectMember);

	/**
	 * 프로젝트 인원 수정 - 복합키 사용
	 * @param projectMember (bizId와 bizUserId 포함)
	 * @return
	 */
	public int updateProjectMember(ProjectMemberVO projectMember);

	/**
	 * 프로젝트 인원 삭제 - 복합키 사용
	 * @param projectMember (bizId와 bizUserId 포함)
	 * @return
	 */
	public int deleteProjectMember(ProjectMemberVO projectMember);
}
