package kr.or.ddit.boardComment.service;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.board.comment.service.BoardCommentService;
import kr.or.ddit.vo.BoardCommentVO;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author 임가영
 * @since 2025. 9. 25.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           	  수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 25.     	임가영	          최초 생성
 *
 * </pre>
 */
@SpringBootTest
@Transactional
@Slf4j
class BoardCommentServiceImplTest {
	
	@Autowired
	BoardCommentService service;
	
	// 댓글
	BoardCommentVO boardComment;
	// 대댓글
	BoardCommentVO boardComment2;
		
	@BeforeEach
	void setBefore() {
		boardComment = new BoardCommentVO();
		boardComment.setPstId("PST0000033");
		boardComment.setCrtUserId("a001");
		boardComment.setContents("댓글 테스트 중");
		boardComment.setUpCmntSqn(null);
			
		assertNotEquals(0, service.createBoardComment(boardComment));
		log.info("======> boardCommnet : {}", boardComment);
	}

	@Test
	void testReadBoardCommentList() {
		boardComment2 = new BoardCommentVO();
		boardComment2.setPstId("PST0000033");
		boardComment2.setUpCmntSqn("35");
		boardComment2.setCrtUserId("a001");
		boardComment2.setContents("대댓글 테스트 중");
		
		service.createBoardComment(boardComment2);
		
		List<BoardCommentVO> boardCommentList = service.readBoardCommentList(boardComment.getPstId());
		
		assertNotEquals(0, boardCommentList.size());
	}

	@Test
	void testCreateBoardComment() {
		List<BoardCommentVO> boardCommentList = service.readBoardCommentList(boardComment.getPstId());
		
		assertNotEquals(0, boardCommentList.size());
	}

	@Test
	void testModifyBoardComment() {
		boardComment.setContents("수정..");
		boolean success = service.modifyBoardComment(boardComment);
		
		log.info("======> boardCommnet 수정 : {}", boardComment);
		assertTrue(success);
	}

	@Test
	void testRemoveBoardComment() {
		boolean success = service.removeBoardComment("1");
		
		log.info("======> boardCommnet 삭제 : {}", boardComment);
		assertTrue(success);
	}

}
