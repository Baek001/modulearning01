package kr.or.ddit.mybatis.mapper;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

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
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 25.     	임가영	       최초 생성
 *
 * </pre>
 */
@SpringBootTest
@Transactional
@Slf4j
class BoardCommentMapperTest {

	@Autowired
	BoardCommentMapper mapper;
	
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
		
		assertNotEquals(0, mapper.insertBoardComment(boardComment));
		log.info("======> boardCommnet : {}", boardComment);
	}
	
	@Test
	void testInsertUpComment() {
		boardComment2 = new BoardCommentVO();
		boardComment2.setPstId("PST0000033");
		boardComment2.setUpCmntSqn("35");
		boardComment2.setCrtUserId("a001");
		boardComment2.setContents("대댓글 테스트 중");
		
		mapper.insertBoardComment(boardComment2);
		
		List<BoardCommentVO> boardCommentList = mapper.selectBoardCommentList(boardComment.getPstId());
		
		assertNotEquals(0, boardCommentList.size());
	}
	
	@Test
	void testSelectBoardCommentList() {
		List<BoardCommentVO> boardCommentList = mapper.selectBoardCommentList(boardComment.getPstId());
		
		assertNotEquals(0, boardCommentList.size());
	}

	@Test
	void testUpdateBoardComment() {
		boardComment.setContents("수정..");
		int rowcnt = mapper.updateBoardComment(boardComment);
		
		log.info("======> boardCommnet 수정 : {}", boardComment);
		assertNotEquals(0, rowcnt);
	}

	@Test
	void testDeleteBoardComment() {
		int rowcnt = mapper.deleteBoardComment("1");
		
		log.info("======> boardCommnet 삭제 : {}", boardComment);
		assertNotEquals(0, rowcnt);
	}

}
