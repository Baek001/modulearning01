package kr.or.ddit.mybatis.mapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.vo.UserFileMappingVO;
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
class UserFileMappingMapperTest {
	
	@Autowired
	UserFileMappingMapper mapper;
	
	UserFileMappingVO vo;
	
//	@BeforeEach
//	void setUpBefore() {
//		
//	}
//	@Test
//	void testSelectUserFileMappingList() {
//		List<UserFileMappingVO> list = mapper.selectUserFileMappingList("a001");
//		assertNotEquals(list, 0);
//		log.info("Result : " + list);
//	}
//
//	@Test
//	void testSelectUserFileMapping() {
//		UserFileMappingVO selectVo = mapper.selectUserFileMapping("a001","FILE00000002"); 
//		assertNotNull(selectVo);
//		log.info("Result : " + selectVo);
//	}

}
