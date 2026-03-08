package kr.or.ddit.board.comment.service;

import java.util.List;

import kr.or.ddit.vo.BoardCommentVO;

/**
 * 
 * @author 임가영
 * @since 2025. 9. 25.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 25.     	임가영	       	최초 생성
 *	2025. 9. 29.		임가영			readBoardCommentTotalCount() (댓글 수) 메소드 추가 / readBoardCommentDetail() (상세조회) 메소드 추가
 *	2025. 9. 30.		임가영			readBoardCommentDetail() / readBoardReplyDetail() 메소드 분리
 * </pre>
 */
public interface BoardCommentService {
	
	/**
	 * 게시물 하나에 대한 댓글 수
	 * @param boardComment 게시물 Id 를 담은 vo
	 * @return 댓글 수
	 */
	public int readBoardCommentTotalCount(BoardCommentVO boardComment);
	
	/**
	 * 게시물 하나에 대한 모든 댓글 조회
	 * @param pstId 게시물 Id
	 * @return 조회 결과 없으면 list.size() == 0.
	 */
	public List<BoardCommentVO> readBoardCommentList(String pstId);
	
	/**
	 * 대댓글 하나에 대한 상세 정보 조회
	 * @param boardCommentVO 게시물 Id 와 상위 댓글 sqn 을 담은 vo
	 * @return 조회 결과 없으면 null
	 */
	public BoardCommentVO readBoardReplyDetail(BoardCommentVO boardCommentVO);
	
	/**
	 * 댓글 하나에 대한 상세 정보 조회
	 * @param boardCommentVO 댓글 sqn 을 담은 vo
	 * @return 조회 결과 없으면 null
	 */
	public BoardCommentVO readBoardCommentDetail(BoardCommentVO boardCommentVO);
	
	/**
	 * 댓글 등록.
	 * @param boardComment
	 * @return 성공하면 true, 실패하면 false
	 */
	public boolean createBoardComment(BoardCommentVO boardComment);
	
	/**
	 * 댓글 수정.
	 * @param boardComment
	 * @return 성공하면 true, 실패하면 false
	 */
	public boolean modifyBoardComment(BoardCommentVO boardComment);
	
	/**
	 * 댓글 삭제. (삭제 여부 N 으로 변경)
	 * @param CmntId
	 * @return 성공하면 true, 실패하면 false
	 */
	public boolean removeBoardComment(String cmntId);
}
