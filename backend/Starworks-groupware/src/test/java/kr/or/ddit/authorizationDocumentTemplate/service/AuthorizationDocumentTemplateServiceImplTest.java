package kr.or.ddit.authorizationDocumentTemplate.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.approval.template.service.AuthorizationDocumentTemplateService;
import kr.or.ddit.vo.AuthorizationDocumentTemplateVO;

@Transactional
@SpringBootTest
class AuthorizationDocumentTemplateServiceImplTest {

	@Autowired
	AuthorizationDocumentTemplateService service;

	@Test
	void testCreateAuthDocumentTemplate() {
		AuthorizationDocumentTemplateVO authDocumentTemplate = new AuthorizationDocumentTemplateVO();
		authDocumentTemplate.setAtrzDocTmplId("ABCDEFG111");
		authDocumentTemplate.setAtrzDocCd("A101");
		authDocumentTemplate.setAtrzDocTmplNm("휴가원");
		assertTrue(service.createAuthDocumentTemplate(authDocumentTemplate));
	}

	@Test
	void testReadAuthDocumentTemplateList() {
//		assertNotNull(service.readAuthDocumentTemplateList());
	}

	@Test
	void testReadAuthDocumentTemplate() {
		assertDoesNotThrow(()-> service.readAuthDocumentTemplate("ATRZDOC999"));
	}

	@Test
	void testRemoveAuthDocumentTemplate() {
//		assertTrue(service.removeAuthDocumentTemplate("ATRZDOC999"));
	}

}
