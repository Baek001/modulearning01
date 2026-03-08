package kr.or.ddit.mybatis.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

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
 *   수정일      			수정자            수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 25.     	임가영	        최초 생성
 *	2025. 9. 26.		임가영			selectBoardCommentTotalCount() (댓글 수) 메소드 추가
 *	2025. 9. 29.		임가영			selectBoardReplyDetail (댓글상세정보) 메소드 추가
 * </pre>
 */
@Mapper
public interface BoardCommentMapper {
	
	/**
	 * 게시물 하나에 대한 댓글 수
	 * @param boardComment 게시물 Id 를 담은 vo
	 * @return 댓글 수
	 */
	public int selectBoardCommentTotalCount(BoardCommentVO boardComment);

	/**
	 * 게시물 하나에 대한 모든 댓글 조회
	 * @param pstId 게시물 Id
	 * @return 조회 결과 없으면 list.size() == 0.
	 */
	public List<BoardCommentVO> selectBoardCommentList(String pstId);
	
	/**
	 * 대댓글 하나에 대한 상세 정보 조회
	 * @param boardCommentVO 게시물 Id 와 상위댓글 sqn 을 담은 vo
	 * @return 조회 결과 없으면 null
	 */
	public BoardCommentVO selectBoardReplyDetail(BoardCommentVO boardCommen);
	
	/**
	 * 댓글 하나에 대한 상세 정보 조회
	 * @param boardComment 댓글 sqn 을 담은 vo
	 * @return 조회 결과 없으면 null
	 */
	public BoardCommentVO selectBoardCommentDetail(BoardCommentVO boardComment);
	
	/**
	 * 댓글 등록.
	 * @param boardComment 댓글 정보를 담은 vo
	 * @return 성공한 레코드 수
	 */
	public int insertBoardComment(BoardCommentVO boardComment);
	
	/**
	 * 댓글 수정.
	 * @param boardComment 댓글 수정 정보를 담은 vo
	 * @return 성공한 레코드 수
	 */
	public int updateBoardComment(BoardCommentVO boardComment);
	
	/**
	 * 댓글 삭제. (삭제 여부 N 으로 변경)
	 * @param CmntId 댓글 Id
	 * @return 성공한 레코드 수
	 */
	public int deleteBoardComment(String cmntId);
}
