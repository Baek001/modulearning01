package kr.or.ddit.projectBoard.service;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.comm.exception.EntityNotFoundException;
import kr.or.ddit.project.board.service.ProjectBoardService;
import kr.or.ddit.vo.ProjectBoardVO;
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
 *  2025.10. 02.     	장어진	          기능 제작을 위한 각종 코드 추가
 *
 * </pre>
 */
@Slf4j
@Transactional
@SpringBootTest
class ProjectBoardServiceImplTest {

	@Autowired
	ProjectBoardService service;

	ProjectBoardVO vo;

	// java.lang.NullPointerException: Cannot invoke "org.springframework.security.core.Authentication.getPrincipal()" because "authentication" is null
	@BeforeEach
	void setBefore() {
		vo = new ProjectBoardVO();
		vo.setBizId("BIZ202599999");
		vo.setPstTtl("TEST TITLE");
		vo.setCrtUserId("a001");
		
		assertNotEquals(0, service.createProjectBoard(vo));
		log.info("Result CREATE : {}", vo);
	}
	
//	@Test
//	void testReadProjectBoardListNonPaging() {
//		String bizId = vo.getBizId();
//		List<ProjectBoardVO> list = service.readProjectBoardListNonPaging(bizId);
//
//		log.info("Result READ LIST : {}", list);
//		assertNotEquals(0, list.size());
//	}	

	@Test 
	void testRemoveProjectBoard() {
		assertTrue(service.removeProjectBoard(vo.getBizPstId()));
	};
	
	@Test
	void testReadProjectBoard() {
		ProjectBoardVO insertVO = service.readProjectBoard(vo.getBizPstId());

		log.info("Result READ ONE : {}", insertVO);
		assertNotNull(insertVO);

		assertThrows(EntityNotFoundException.class, () -> 
			service.readProjectBoard("aaaa")
		);
	}

	@Test
	void testModifyProjectBoard() {
		vo.setContents("수정된 TEST CONTENT");
		vo.setLastChgUserId(vo.getCrtUserId());

		assertNotEquals(0, service.modifyProjectBoard(vo));
		log.info("Result MODIFY : {}", vo);
	}

}
