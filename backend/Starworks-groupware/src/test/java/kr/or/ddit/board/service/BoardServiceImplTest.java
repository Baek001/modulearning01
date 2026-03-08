package kr.or.ddit.board.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.board.community.service.BoardService;
import kr.or.ddit.comm.exception.EntityNotFoundException;
import kr.or.ddit.vo.BoardVO;
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
class BoardServiceImplTest {
	
	@Autowired
	BoardService service;
	
	BoardVO board;
	@BeforeEach
	void setBefore() {
		board = new BoardVO();
		board.setBbsCtgrCd("F101");
		board.setPstTtl("테스트 게시글 제목");
		board.setContents("테스트 게시글 내용입니다.");
		board.setCrtUserId("a001");
		
		service.createBoard(board);
	}

	// 페이징 처리라서 아직 테스트 안 함.
//	@Test
//	void testReadNoticeList() {
//		List<BoardVO> boardList = service.read();
//		
//		log.info("======> boardList : {}",boardList);
//		assertNotEquals(0, boardList.size());
//	}

//	@Test
//	void testReadCommunityListNonPaging() {
//		List<BoardVO> boardList = service.readCommunityListNonPaging();
//		
//		log.info("======> boardList : {}",boardList);
//		assertNotEquals(0, boardList.size());
//	}

//	@Test
//	void testReadcommunityListCategoryNonPaging() {
//		List<BoardVO> boardList = service.readcommunityListCategoryNonPaging("F105");
//		
//		log.info("======> boardList : {}",boardList);
//		assertNotEquals(0, boardList.size());
//	}

	@Test
	void testReadBoard() {
		assertNotNull(service.readBoard("PST0000034"));
		assertThrows(EntityNotFoundException.class, () -> 
			service.readBoard("asdfasdf")
		);
	}

	@Test
	void testModifyBoard() {
		board.setContents("수정한 게시글 내용~~");
		assertTrue(service.modifyBoard(board));
	}

	@Test
	void testRemoveBoard() {
		assertTrue(service.removeBoard(board.getPstId()));
	}

}
