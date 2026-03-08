package kr.or.ddit.messengerContent.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.messenger.content.service.MessengerContentService;
import kr.or.ddit.vo.MessengerContentVO;

@Transactional
@SpringBootTest
class MessengerContentServiceImplTest {

	@Autowired
	MessengerContentService service;
	
	@Test
	void testCreateMessengerContent() {
		MessengerContentVO mesContent = new MessengerContentVO();
		mesContent.setMsgContId("MSG00000000012");
		mesContent.setUserId("2020005");
		mesContent.setMsgrId("MSGR00000002");
		assertTrue(service.createMessengerContent(mesContent));
	}

	@Test
	void testReadMessengerContentList() {
		assertNotNull(service.readMessengerContentList());
	}

	@Test
	void testRemoveMessengerContent() {
		boolean mesContent = service.removeMessengerContent("MSG00000000002");
		assertTrue(mesContent);
	}

}
