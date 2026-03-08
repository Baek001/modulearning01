package kr.or.ddit.emailSenderParty.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.email.sender.service.EmailSenderPartyService;
import kr.or.ddit.vo.EmailSenderPartyVO;

@Transactional
@SpringBootTest
class EmailSenderPartyServiceImplTest {

	@Autowired
	EmailSenderPartyService service;
	
	@Test
	void testRegisterEmailSenderParty() {
		EmailSenderPartyVO vo = new EmailSenderPartyVO();
		vo.setEmailContId("MAIL00000003");
		assertTrue(service.registerEmailSenderParty(vo));
	}

}
