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

import kr.or.ddit.vo.AuthorizationDocumentReceiverVO;

@Transactional
@SpringBootTest
class AuthorizationDocumentReceiverMapperTest {
	Logger log = LoggerFactory.getLogger(AuthorizationDocumentReceiverMapperTest.class);
	
	@Autowired
	AuthorizationDocumentReceiverMapper mapper;
	
	@BeforeEach
	void testInsertAuthDocumentReceiver() {
		AuthorizationDocumentReceiverVO authReceiver = new AuthorizationDocumentReceiverVO();
		authReceiver.setAtrzDocId("ATRZ000000000020");
		authReceiver.setAtrzRcvrId("a001");
		int rowcnt = mapper.insertAuthDocumentReceiver(authReceiver);
		assertEquals(1, rowcnt);
	}

	@Test
	void testSelectAuthDocumentReceiverList() {
		mapper.selectAuthDocumentReceiverList().forEach(System.out::println);
	}

	@Test
	void testSelectAuthDocumentReceiver() {
		AuthorizationDocumentReceiverVO authReceiver = mapper.selectAuthDocumentReceiver("ATRZ000000000020");
		assertNotNull(authReceiver);
	}

}
