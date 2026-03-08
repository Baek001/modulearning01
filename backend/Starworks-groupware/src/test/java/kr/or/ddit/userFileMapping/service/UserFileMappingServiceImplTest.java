package kr.or.ddit.userFileMapping.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.document.users.service.DocumentUserFileService;
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
class UserFileMappingServiceImplTest {

	@Autowired
	DocumentUserFileService service;
	
//	@Test
//	void testReadUserFileMappingList() {
//		List<UserFileMappingVO> list = service.readUserFileMappingList("a001");
//		
//		log.info("Result READ LIST : {}", list);
//		assertNotEquals(0, list.size());
//	}
//
//	@Test
//	void testReadUserFileMapping() {
//		UserFileMappingVO selectVo = service.readUserFileMapping("a001","FILE00000002"); 
//
//		log.info("Result READ ONE : {}", selectVo);
//		assertNotNull(selectVo);
//
//		assertThrows(EntityNotFoundException.class, () -> 
//			service.readUserFileMapping("aaaa", "FILE00000002")
//		);
//	}

}
