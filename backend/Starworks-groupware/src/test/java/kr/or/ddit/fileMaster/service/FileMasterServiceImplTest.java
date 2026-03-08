package kr.or.ddit.fileMaster.service;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.comm.exception.EntityNotFoundException;
import kr.or.ddit.comm.file.service.FileMasterService;
import kr.or.ddit.vo.FileMasterVO;
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
class FileMasterServiceImplTest {
	
	@Autowired
	FileMasterService service;

	FileMasterVO vo;

	@BeforeEach
	void setBefore() {
		vo = new FileMasterVO();
		vo.setCrtUserId("a001");
		
		assertNotEquals(0, service.createFileMaster(vo));
		log.info("Result CREATE : {}", vo);
	}
	
	@Test
	void testReadFileMasterList() {
		List<FileMasterVO> list = service.readFileMasterList();

		log.info("Result READ LIST : {}", list);
		assertNotEquals(0, list.size());
	}

	@Test
	void testReadFileMaster() {
		FileMasterVO insertVO = service.readFileMaster(vo.getFileId());

		log.info("Result READ ONE : {}", insertVO);
		assertNotNull(insertVO);

		assertThrows(EntityNotFoundException.class, () -> 
			service.readFileMaster("aaaa")
		);
	}

	@Test
	void testModifyFileMasterDelyn() {
		vo.setDelYn("Y");

		assertNotEquals(0, service.modifyFileMasterDelyn(vo));
		log.info("Result MODIFY : {}", vo);
	}

}
