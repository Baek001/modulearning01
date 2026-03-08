package kr.or.ddit.messengerUser.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.messenger.user.service.MessengerUserService;
import kr.or.ddit.vo.MessengerUserVO;

@Transactional
@SpringBootTest
class MessengerUserServiceImplTest {

	@Autowired
	MessengerUserService service;

	@Test
	void testCreateMessengerUser() {
		MessengerUserVO mesUser = new MessengerUserVO();
		mesUser.setUserId("2020010");
		mesUser.setMsgrId("MSGR00000002");
		/* mesUser.setJoinDt(LocalDateTime.now()); */
		assertTrue(service.createMessengerUser(mesUser));
	}

	@Test
	void testReadMessengerUserList() {
		assertNotNull(service.readMessengerUserList());
	}

	@Test
	void testReadMessengerUser() {
		assertDoesNotThrow(()->service.readMessengerUser("2020004"));
	}

	@Test
	void testRemoveMessengerUser() {
		boolean mesUser = service.removeMessengerUser("2020004");
		assertTrue(mesUser);
	}

}
