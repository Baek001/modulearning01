package kr.or.ddit.project.comment.service;

import java.util.List;

import kr.or.ddit.vo.ProjectBoardCommentVO;


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
 *  2025.10.  6.     	장어진	          BoardComment 참고해서 메소드 추가
 *
 * </pre>
 */
public interface ProjectBoardCommentService {
	
	/**
	 * 게시물 하나에 대한 댓글 수
	 * @param pbcVO : Project Board Comment VO 객체
	 * @return 댓글 수 
	 */
	public int readProjectBoardCommentTotalCount(ProjectBoardCommentVO pbcVO);
	
	/**
	 * 프로젝트 게시물 댓글 목록 조회.
	 * @param bizPstId : 
	 * @return 조회 결과 없으면 list.size() == 0
	 */
	public List<ProjectBoardCommentVO> readProjectBoardCommentList(String bizPstId);
	
	/**
	 * 프로젝트 게시물 대댓글 조회.
	 * @param pbcVO : Project Board Comment VO 객체
	 * @return 조회 결과 없으면 EntityNotFoundException
	 */
	public ProjectBoardCommentVO readProjectBoardReply(ProjectBoardCommentVO pbcVO);
	
	/**
	 * 프로젝트 게시물 댓글 단건 조회.
	 * @param pbcVO : Project Board Comment VO 객체
	 * @return 조회 결과 없으면 EntityNotFoundException
	 */
	public ProjectBoardCommentVO readProjectBoardComment(ProjectBoardCommentVO pbcVO);
	
	/**
	 * 프로젝트 게시물 댓글 추가
	 * @param pbcVO : Project Board Comment VO 객체
	 * @return 성공 시 true, 실패 시 false
	 */
	public boolean createProjectBoardComment(ProjectBoardCommentVO pbcVO);
	
	/**
	 * 프로젝트 게시물 댓글 수정
	 * @param pbcVO : Project Board Comment VO 객체
	 * @return 성공 시 true, 실패 시 false
	 */
	public boolean modifyProjectBoardComment(ProjectBoardCommentVO pbcVO);
	
	/**
	 * 프로젝트 게시물 댓글 삭제 (soft delete)
	 * @param bizCmntId : Project 게시물 댓글 ID
	 * @return 성공 시 true, 실패 시 false
	 */
	public boolean removeProjectBoardComment(String bizCmntId);
}
