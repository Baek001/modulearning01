package kr.or.ddit.authorizationDocument.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.approval.document.service.AuthorizationDocumentService;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author 임가영
 * @since 2025. 9. 26.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *     수정일      		수정자               수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 26.     	임가영	           최초 생성
 *
 * </pre>
 */
@SpringBootTest
@Transactional
@Slf4j
class AuthorizationDocumentServiceImplTest {
	
	@Autowired
	AuthorizationDocumentService service;

//	@Test
//	void testReadAuthorizationDocumentListUser() {
//		assertNotEquals(0, service.readAuthorizationDocumentListUser("a001"));
//	}
//
//	@Test
//	void testReadAuthorizationDocumentListDepart() {
//		assertNotEquals(0, service.readAuthorizationDocumentListDepart("a001"));
//	}


}
