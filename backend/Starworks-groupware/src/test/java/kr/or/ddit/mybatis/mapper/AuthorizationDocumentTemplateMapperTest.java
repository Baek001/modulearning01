package kr.or.ddit.mybatis.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.vo.AuthorizationDocumentTemplateVO;

@Transactional
@SpringBootTest
class AuthorizationDocumentTemplateMapperTest {

	Logger log = LoggerFactory.getLogger(AuthorizationDocumentTemplateMapperTest.class);

	@Autowired
	AuthorizationDocumentTemplateMapper mapper;

	@BeforeEach
	void testInsertAuthDocumentTemplate() {
		AuthorizationDocumentTemplateVO AuthDocumentTemplate = new AuthorizationDocumentTemplateVO();
		AuthDocumentTemplate.setAtrzDocTmplId("ABCDEFG111");
		AuthDocumentTemplate.setAtrzDocCd("A101");
		AuthDocumentTemplate.setAtrzDocTmplNm("휴가원");
		int rowcnt = mapper.insertAuthDocumentTemplate(AuthDocumentTemplate);
		assertEquals(1, rowcnt);


	}

	@Test
	void testSelectAuthDocumentTemplateList() {
		mapper.selectAuthDocumentTemplateList().forEach(System.out::println);
	}

	@Test
	void testSelectAuthDocumentTemplate() {
		AuthorizationDocumentTemplateVO AuthDocumentTemplate = mapper.selectAuthDocumentTemplate("ATRZDOC999");
		assertNotNull(AuthDocumentTemplate);
	}

	@Test
	void testDeleteAuthDocumentTemplate() {
//		assertEquals(1, mapper.deleteAuthDocumentTemplate("ABCDEFG111"));
	}

}
