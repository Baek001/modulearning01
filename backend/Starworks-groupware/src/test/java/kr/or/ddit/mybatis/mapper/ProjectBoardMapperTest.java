package kr.or.ddit.mybatis.mapper;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.vo.ProjectBoardVO;
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
class ProjectBoardMapperTest {
	@Autowired
	ProjectBoardMapper mapper;
	
	ProjectBoardVO vo;
	
	@BeforeEach
	void setUpBefore() {
		vo = new ProjectBoardVO();
		vo.setBizId("BIZ202599999");
		vo.setPstTtl("TEST TITLE");	
		vo.setCrtUserId("a001");

		int res = mapper.insertProjectBoard(vo); 
		assertTrue(res > 0);
	}
	@Test
	void testSelectProjectBoardListNonPaging() {
		String bizId = vo.getBizId();
		List<ProjectBoardVO> list = mapper.selectProjectBoardListNonPaging(bizId);
		assertNotEquals(0, list);
		log.info("Result : " + list);
	}

	@Test
	void testSelectProjectBoard() {
		String insertId = vo.getBizPstId();
		ProjectBoardVO selectVo = mapper.selectProjectBoard(insertId); 
		assertNotNull(selectVo);
		log.info("Result : " + selectVo);
	}

	@Test
	void testUpdateProjectBoard() {
		vo.setContents("수정된 TEST CONTENT");
		vo.setLastChgUserId(vo.getCrtUserId());
		int res = mapper.updateProjectBoard(vo);
		assertTrue(res > 0);
		String insertedId = vo.getBizPstId();
		ProjectBoardVO selectVO = mapper.selectProjectBoard(insertedId); 
		log.info("Result : " + selectVO);		
	}

}
