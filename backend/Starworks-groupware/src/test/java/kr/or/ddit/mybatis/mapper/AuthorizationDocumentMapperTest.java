package kr.or.ddit.mybatis.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.vo.AuthorizationDocumentVO;

/**
 * 
 * @author 윤서현
 * @since 2025. 9. 25.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자              수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 25.     	윤서현	          최초 생성
 *  2025. 9. 26.		임가영			  전자결재 개인보관함, 전체보관함 테스트케이스 추가
 *
 * </pre>
 */
@Transactional
@SpringBootTest
class AuthorizationDocumentMapperTest {

	Logger log = LoggerFactory.getLogger(AuthorizationDocumentMapperTest.class);
	
	@Autowired
	AuthorizationDocumentMapper mapper;
	
	@BeforeEach
	void testInsertAuthDocument() {
		AuthorizationDocumentVO authDoc = new AuthorizationDocumentVO();
		authDoc.setAtrzDocId("ATRZ000000000321");
		authDoc.setAtrzUserId("a001");
		authDoc.setAtrzDocTmplId("ATRZDOC999");
		int rowcnt = mapper.insertAuthDocument(authDoc);
		assertEquals(1, rowcnt);
	}

	@Test
	void testSelectAuthDocumentList() {
		mapper.selectAuthDocumentList().forEach(System.out::println);
	}

////	@Test
//	void testSelectAuthDocument() {
//		AuthorizationDocumentVO authDoc = mapper.selectAuthDocument("ATRZ000000000002");
//		assertNotNull(authDoc);
//	}
//
////	@Test
//	void testUpdateAuthDocument() {
//		AuthorizationDocumentVO authDoc = mapper.selectAuthDocument("ATRZ000000000002");
//		authDoc.setDelYn("Y");
//		assertEquals(1, mapper.updateAuthDocument(authDoc));
//	}
	
//	@Test
//	void testSelectAuthorizationDocumentListUser() {
//		assertNotEquals(0, mapper.selectAuthorizationDocumentListUser("a001").size());
//	}
//
//	@Test
//	void testSelectAuthorizationDocumentListDepart() {
//		assertNotEquals(0, mapper.selectAuthorizationDocumentListDepart("a001").size());
//	}
}
