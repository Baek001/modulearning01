package kr.or.ddit.project.member.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import kr.or.ddit.comm.exception.EntityNotFoundException;
import kr.or.ddit.mybatis.mapper.ProjectMemberMapper;
import kr.or.ddit.vo.ProjectMemberVO;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author 김주민
 * @since 2025. 9. 25.
 * @see projectMemberServiceImpl
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
@Service
@RequiredArgsConstructor
public class projectMemberServiceImpl implements projectMemberService {

	@Autowired
	private ProjectMemberMapper mapper;

	/**
	 * 프로젝트 인원 추가
	 */
	@Override
	public boolean createProjectMember(ProjectMemberVO newProjectMember) {
		int rowcnt = mapper.insertProjectMember(newProjectMember);
		if(rowcnt == 0) {
			return false;
		}
		return true;
	}

	/**
	 * 프로젝트 인원 목록 조회
	 */
	@Override
	public List<ProjectMemberVO> readProjectMemberList(String bizId) {
		return mapper.selectProjectMemberByProject(bizId);
	}

	/**
	 * 사용자 참여 프로젝트 목록 조회
	 */
	@Override
	public List<ProjectMemberVO> readProjectList(String bizUserId) {
		return mapper.selectProjectByProjectMember(bizUserId);
	}

	/**
	 * 사용자 참여 프로젝트 목록 중 진행중인 목록만 조회
	 */
	@Override
	public List<ProjectMemberVO> readProjectListOnlyB302(String bizUserId) {
		return mapper.selectProjectByProjectMemberOnlyB302(bizUserId);
	}

	/**
	 * 프로젝트 인원 단건 조회
	 */
	@Override
	public ProjectMemberVO readProjectMember(ProjectMemberVO projectMember) {
		ProjectMemberVO result = mapper.selectProjectMember(projectMember);
		if(result == null) {
			throw new EntityNotFoundException(result);
		}
		return result;
	}

	/**
	 * 프로젝트 인원 수정
	 */
	@Override
	public boolean modifyProjectMember(ProjectMemberVO projectMember) {
		int rowcnt = mapper.updateProjectMember(projectMember);
		if(rowcnt == 0) {
			return false;
		}
		return true;
	}

	/**
	 * 프로젝트 인원 삭제
	 */
	@Override
	public boolean removeProjectMember(ProjectMemberVO projectMember) {
		int rowcnt = mapper.deleteProjectMember(projectMember);
		if(rowcnt == 0) {
			return false;
		}
		return true;
	}
}
