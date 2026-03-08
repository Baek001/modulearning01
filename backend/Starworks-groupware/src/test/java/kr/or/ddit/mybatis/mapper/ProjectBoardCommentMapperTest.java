package kr.or.ddit.mybatis.mapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.vo.ProjectBoardCommentVO;
import lombok.extern.slf4j.Slf4j;
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
 *
 * </pre>
 */
@Slf4j
@Transactional
@SpringBootTest
class ProjectBoardCommentMapperTest {
	@Autowired
	ProjectBoardCommentMapper mapper;
	
	ProjectBoardCommentVO vo;
	
//	@BeforeEach
//	void setUpBefore() {
//		vo = new ProjectBoardCommentVO();
//		vo.setBizPstId("BIZ202599999000001");
//		vo.setCrtUserId("a001");
//
//		int res = mapper.insertProjectBoardComment(vo); 
//		assertTrue(res > 0);
//	}
//	
//	@Test
//	void testSelectProjectBoardCommentList() {
//		List<ProjectBoardCommentVO> list = mapper.selectProjectBoardCommentList("");
//		assertNotEquals(list, 0);
//		log.info("Result : " + list);
//	}
//
//	@Test
//	void testSelectProjectBoardComment() {
//		String insertId = vo.getBizCmntId();
//		ProjectBoardCommentVO selectVo = mapper.selectProjectBoardComment(insertId); 
//		assertNotNull(selectVo);
//		log.info("Result : " + selectVo);
//	}
//
//	@Test
//	void testUpdateProjectBoardComment() {
//		vo.setContents("수정된 TEST CONTENT");
//		int res = mapper.updateProjectBoardComment(vo);
//		assertTrue(res > 0);
//		String insertedId = vo.getBizCmntId();
//		ProjectBoardCommentVO selectVO = mapper.selectProjectBoardComment(insertedId); 
//		log.info("Result : " + selectVO);
//	}

}
