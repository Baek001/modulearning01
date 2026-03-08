package kr.or.ddit.mybatis.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.vo.BoardVO;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Transactional
@SpringBootTest
class BoardMapperTest {
	
	@Autowired
	BoardMapper mapper;
	
	BoardVO board;
	@BeforeEach
    void setUpBefore() {
		board = new BoardVO();
		board.setBbsCtgrCd("F101");
		board.setPstTtl("테스트 게시글 제목");
		board.setContents("테스트 게시글 내용입니다.");
		board.setPstId("PST0000002");
		board.setCrtUserId("a001");
		
	}

//	@Test
//	void testSelectTotalRecord() {
//		fail("Not yet implemented");
//	}

//	@Test
//	void testSelectBoardList() {
//		
//	}
	

	@Test
	void testSelectBoard() {
		board = mapper.selectBoard(board.getPstId());
		assertNotNull(board);
		log.info("조회 결과 : {}", board);
	}

	@Test
	void testInsertBoard() {
		fail("Not yet implemented");
	}

	@Test
	void testUpdateBoard() {
		BoardVO board =mapper.selectBoard("PST0000002");
		board.setPstTtl("수정한 제목");
		assertEquals(1, mapper.updateBoard(board));
	}

	@Test
	void testDeleteBoard() {
		int rowcnt = mapper.deleteBoard("PST0000047");
		assertEquals(1, rowcnt);
	}
	
	@Test
	void testSelectNoticeListNonPaging() {
		List<BoardVO> boardList = mapper.selectNoticeListNonPaging();
		log.info("======> 공지사항 boardList : {}", boardList);
		
		assertNotEquals(0, boardList.size());
	}
	
//	@Test
//	void testSelectCommunityListNonPaging() {
//		List<BoardVO> boardList = mapper.selectCommunityListNonPaging();
//		log.info("======> 공지사항 제외 boardList : {}", boardList);
//		
//		assertNotEquals(0, boardList.size());
//	}
//
//	@Test
//	void testSelectcommunityListCategoryNonPaging() {
//		List<BoardVO> boardList1 = mapper.selectcommunityListCategoryNonPaging("F102");
//		log.info("======> F102 boardList : {}", boardList1);
//		
//		List<BoardVO> boardList2 = mapper.selectcommunityListCategoryNonPaging("F105");
//		log.info("======> F105 boardList : {}", boardList2);
//		
//		assertEquals(0, boardList1.size());
//		assertNotEquals(0, boardList2.size());
//	}
//	
//	@Test
//	void testSelectCommunityTotalRecord() {
//		int cnt = mapper.selectCommunityTotalRecord();
//		log.info("======> cnt : {}", cnt);
//	}
//	
//	@Test
//	void testSelectCommunityList() {
//		String bbsCtgrCd = "";
//		PaginationInfo<BoardVO> paging = new PaginationInfo<>();
//		int totalRecord = mapper.selectCommunityTotalRecord(bbsCtgrCd);
//		log.info("======> totalRecord : {}", totalRecord);
//		paging.setTotalRecord(totalRecord);
//		paging.setCurrentPage(1);
//		
//		
//		SimpleSearch simpleSearch = new SimpleSearch();
//		simpleSearch.setSearchType("title");
//		simpleSearch.setSearchWord("사내");
//		
//		paging.setSimpleSearch(simpleSearch);
//		
//		Map<String, Object> paramMap = new HashMap<>();
//		paramMap.put("paging", paging);
//		paramMap.put("bbsCtgrCd", bbsCtgrCd);
//		List<BoardVO> boardList = mapper.selectCommunityList(paramMap);
//		
//		log.info("======> startRow : {}", paging.getStartRow());
//		log.info("======> endRow : {}", paging.getEndRow());
//		log.info("======> boardList : {}", boardList);
//	}

}
