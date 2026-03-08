package kr.or.ddit.authorizationDocumentReceiver.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.approval.receiver.service.AuthorizationDocumentReceiverService;
import kr.or.ddit.vo.AuthorizationDocumentReceiverVO;

@Transactional
@SpringBootTest
class AuthorizationDocumentReceiverServiceImplTest {

	@Autowired
	AuthorizationDocumentReceiverService service;
	
	@Test
	void testCreateAuthDocumentReceiver() {
		AuthorizationDocumentReceiverVO authReceiver = new AuthorizationDocumentReceiverVO();
		authReceiver.setAtrzDocId("ATRZ000000000020");
		authReceiver.setAtrzRcvrId("a001");
		assertTrue(service.createAuthDocumentReceiver(authReceiver));
	}

	@Test
	void testReadAuthDocumentReceiverList() {
		assertNotNull(service.readAuthDocumentReceiverList());
	}

	@Test
	void testReadAuthDocumentReceiver() {
		assertDoesNotThrow(()-> service.readAuthDocumentReceiver("ATRZ000000000006"));
	}

}
