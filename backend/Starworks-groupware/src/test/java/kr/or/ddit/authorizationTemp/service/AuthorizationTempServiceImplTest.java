package kr.or.ddit.authorizationTemp.service;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.approval.temp.service.AuthorizationTempService;
import lombok.extern.slf4j.Slf4j;
/**
 * 
 * @author 윤서현
 * @since 2025.10. 6.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *     수정일      		수정자               수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 6.     	윤서현	           최초 생성
 *
 * </pre>
 */
@SpringBootTest
@Transactional
@Slf4j
class AuthorizationTempServiceImplTest {

	@Autowired
	AuthorizationTempService service;
	
	@Test
	void testReadAuthTempList() {
		assertNotEquals(0, service.readAuthTempList());
	}

	@Test
	void testReadAuthTemp() {
		assertNotEquals(0, service.readAuthTemp("A206"));
	}

}
