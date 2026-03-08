package kr.or.ddit.mybatis.mapper;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.vo.AuthorizationTempVO;
/**
 * 
 * @author 윤서현
 * @since 2025. 10. 6.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자              수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 6.     	윤서현	          최초 생성
 *
 * </pre>
 */
@Transactional
@SpringBootTest
class AuthorizationTempMapperTest {

	Logger log = LoggerFactory.getLogger(AuthorizationTempMapperTest.class);
	
	@Autowired
	AuthorizationTempMapper mapper;
	
	@Test
	void testSelectAuthTempList(String loginId) {
		mapper.selectAuthTempList(loginId).forEach(System.out::println);
		
	}

	@Test
	void testSelectAuthTemp() {
		String atrzTempSqn = "A206";
		AuthorizationTempVO authTemp = mapper.selectAuthTemp(atrzTempSqn);
		assertNotNull(authTemp);
		System.out.println(authTemp.getAtrzDocTtl());
				
	}

}
