package kr.or.ddit.fileDetail.service;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.comm.file.service.FileDetailService;
import kr.or.ddit.vo.FileDetailVO;
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
class FileDetailServiceImplTest {
	
	@Autowired
	FileDetailService service;
	
//	FileDetailVO vo;
	
//	@BeforeEach
//	void setBefore() {
//		vo = new FileDetailVO();
//		vo.set
//		
//		assertNotEquals(0, service.create(vo));
//		log.info("Result CREATE : {}", vo);
//	}
	
	@Test
	void testReadFileDetailList() {
		List<FileDetailVO> list = service.readFileDetailList("FILE00000002");
		assertNotEquals(list, 0);
		log.info("Result : " + list);		
	}

	@Test
	void testReadFileDetail() {
		FileDetailVO selectVo = service.readFileDetail(1); 
		assertNotNull(selectVo);
		log.info("Result : " + selectVo);		
	}

}
