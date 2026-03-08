package kr.or.ddit.mybatis.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

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
 *  2025.10.  6.     	장어진	          BoardComment 참고해서 메소드 추가
 *
 * </pre>
 */
@Mapper
public interface ProjectBoardCommentMapper {
	
	/**
	 * 게시물 하나에 대한 댓글 수
	 * @param pbcVO : Project Board Comment VO 객체
	 * @return 댓글 수
	 */
	public int selectProjectBoardCommentTotalCount(ProjectBoardCommentVO pbcVO);
	
	/**
	 * 프로젝트 게시물 댓글 목록 조회
	 * @return 조회 결과 없으면 list.size() == 0
	 */
	public List<ProjectBoardCommentVO> selectProjectBoardCommentList(String bizPstId);
	
	/**
	 * 대댓글 하나에 대한 상세 정보 조회
	 * @param pbcVO : Project Board Comment VO 객체
	 * @return 조회 결과 없을 시 null
	 */
	public ProjectBoardCommentVO selectProjectBoardReplyDetail(ProjectBoardCommentVO pbcVO);
	
	/**
	 * 프로젝트 게시물 댓글 단건 조회
	 * @param pbcVO : Project Board Comment VO 객체
	 * @return 조회 결과 없을 시 null
	 */
	public ProjectBoardCommentVO selectProjectBoardComment(ProjectBoardCommentVO pbcVO);
	
	/**
	 * 프로젝트 게시물 댓글 추가
	 * @param pbcVO : Project Board Comment VO 객체
	 * @return 성공 시 1, 실패 시 0
	 */
	public int insertProjectBoardComment(ProjectBoardCommentVO pbcVO);
	
	/**
	 * 프로젝트 게시물 댓글 수정
	 * @param pbcVO : Project Board Comment VO 객체
	 * @return 성공 시 1, 실패 시 0
	 */
	public int updateProjectBoardComment(ProjectBoardCommentVO pbcVO);
	
	/**
	 * 프로젝트 게시물 댓글 삭제 (soft delete)
	 * @param bizCmntId : Project 게시물 댓글 ID
	 * @return 성공 시 1, 실패 시 0
	 */
	public int deleteProjectBoardComment(String bizCmntId);
}
