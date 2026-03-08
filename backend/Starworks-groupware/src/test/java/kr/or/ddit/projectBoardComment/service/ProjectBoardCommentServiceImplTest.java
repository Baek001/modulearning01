package kr.or.ddit.projectBoardComment.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.project.comment.service.ProjectBoardCommentService;
import kr.or.ddit.vo.ProjectBoardCommentVO;
import lombok.extern.slf4j.Slf4j;
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
 *
 * </pre>
 */
@Slf4j
@Transactional
@SpringBootTest
class ProjectBoardCommentServiceImplTest {
	
	@Autowired
	ProjectBoardCommentService service;

	ProjectBoardCommentVO vo;

//	@BeforeEach
//	void setBefore() {
//		vo = new ProjectBoardCommentVO();
//		vo.setBizPstId("BIZ202599999000001");
//		vo.setCrtUserId("a001");
//		
//		assertNotEquals(0, service.createProjectBoardComment(vo));
//		log.info("Result CREATE : {}", vo);
//	}
//	
//	@Test
//	void testReadProjectBoardList() {
//		List<ProjectBoardCommentVO> list = service.readProjectBoardCommentList();
//
//		log.info("Result READ LIST : {}", list);
//		assertNotEquals(0, list.size());
//	}
//
//	@Test
//	void testReadProjectBoard() {
//		ProjectBoardCommentVO insertVO = service.readProjectBoardComment(vo.getBizCmntId());
//
//		log.info("Result READ ONE : {}", insertVO);
//		assertNotNull(insertVO);
//
//		assertThrows(EntityNotFoundException.class, () -> 
//			service.readProjectBoardComment("aaaa")
//		);
//	}
//
//	@Test
//	void testModifyProjectBoard() {
//		vo.setContents("수정된 TEST CONTENT");
//
//		assertNotEquals(0, service.modifyProjectBoardComment(vo));
//		log.info("Result MODIFY : {}", vo);
//	}

}
