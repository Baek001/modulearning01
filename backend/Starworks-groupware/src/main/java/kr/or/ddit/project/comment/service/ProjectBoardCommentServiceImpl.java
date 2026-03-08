package kr.or.ddit.project.comment.service;

import java.util.List;

import org.springframework.stereotype.Service;

import kr.or.ddit.mybatis.mapper.ProjectBoardCommentMapper;
import kr.or.ddit.vo.ProjectBoardCommentVO;
import lombok.RequiredArgsConstructor;
/**
 * 
 * @author 장어진
 * @since 2025. 9. 26.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 26.     	장어진	          최초 생성
 *  2025.10.  6.     	장어진	          BoardComment 참고해서 메소드 추가
 *
 * </pre>
 */
@Service
@RequiredArgsConstructor
public class ProjectBoardCommentServiceImpl implements ProjectBoardCommentService{
	private final ProjectBoardCommentMapper mapper;

	@Override
	public int readProjectBoardCommentTotalCount(ProjectBoardCommentVO pbcVO) {
		return mapper.selectProjectBoardCommentTotalCount(pbcVO);
	}

	@Override
	public List<ProjectBoardCommentVO> readProjectBoardCommentList(String bizPstId) {
		return mapper.selectProjectBoardCommentList(bizPstId);
	}

	@Override
	public ProjectBoardCommentVO readProjectBoardReply(ProjectBoardCommentVO pbcVO) {
		return mapper.selectProjectBoardReplyDetail(pbcVO);
	}

	@Override
	public ProjectBoardCommentVO readProjectBoardComment(ProjectBoardCommentVO pbcVO) {
		return mapper.selectProjectBoardComment(pbcVO);
	}

	@Override
	public boolean createProjectBoardComment(ProjectBoardCommentVO pbcVO) {
		return mapper.insertProjectBoardComment(pbcVO) > 0;
	}

	@Override
	public boolean modifyProjectBoardComment(ProjectBoardCommentVO pbcVO) {
		return mapper.updateProjectBoardComment(pbcVO) > 0;
	}

	@Override
	public boolean removeProjectBoardComment(String bizCmntId) {
		return mapper.deleteProjectBoardComment(bizCmntId) > 0;
	}
	
}
