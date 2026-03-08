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

import kr.or.ddit.vo.FileMasterVO;
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
class FileMasterMapperTest {
	@Autowired
	FileMasterMapper mapper;
	
	FileMasterVO vo;
	
	@BeforeEach
	void setUpBefore() {
		vo = new FileMasterVO();
		vo.setCrtUserId("a001");
		
		int res = mapper.insertFileMaster(vo); 
		assertTrue(res > 0);
	}
	
	@Test
	void testSelectFileMasterList() {
		List<FileMasterVO> list = mapper.selectFileMasterList();
		assertNotEquals(list, 0);
		log.info("Result : " + list);
	}

	@Test
	void testSelectFileMaster() {
		String insertId = vo.getFileId();
		FileMasterVO selectVO = mapper.selectFileMaster(insertId); 
		assertNotNull(selectVO);
		log.info("Result : " + selectVO);
	}

	@Test
	void testUpdateFileMasterDelyn() {
		vo.setDelYn("Y");
		int res = mapper.updateFileMasterDelyn(vo);
		assertTrue(res > 0);
		String insertedId = vo.getFileId();
		FileMasterVO selectVO = mapper.selectFileMaster(insertedId); 
		log.info("Result : " + selectVO);
	}

}
