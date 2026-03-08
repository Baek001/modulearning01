package kr.or.ddit.mybatis.mapper;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.vo.FileDetailVO;
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
class FileDetailMapperTest {
	@Autowired
	FileDetailMapper mapper;
	
	FileDetailVO vo;
	
//	@BeforeEach
//	void setUpBefore() {
//		
//	}
	@Test
	void testSelectFileDetailList() {
		List<FileDetailVO> list = mapper.selectFileDetailList("FILE00000002");
		assertNotEquals(list, 0);
		log.info("Result : " + list);
	}

	@Test
	void testSelectFileDetail() {
		FileDetailVO selectVo = mapper.selectFileDetail(1); 
		assertNotNull(selectVo);
		log.info("Result : " + selectVo);
	}

}
