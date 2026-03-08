package kr.or.ddit.messengerRoom.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.messenger.room.service.MessengerRoomService;
import kr.or.ddit.vo.MessengerRoomVO;

@Transactional
@SpringBootTest
class MessengerRoomServiceImplTest {

	@Autowired
	MessengerRoomService service;
	
	@Test
	void testCreateMessengerRoom() {
		MessengerRoomVO mesRoom = new MessengerRoomVO();
		mesRoom.setMsgrId("MSGR00000011");
		mesRoom.setMsgrNm("TEST ROOM");
		assertTrue(service.createMessengerRoom(mesRoom));
	}

	@Test
	void testReadMessengerRoomList() {
		assertNotNull(service.readMessengerRoomList());
	}

	@Test
	void testReadMessengerRoom() {
		assertDoesNotThrow(()->service.readMessengerRoom("MSGR00000002"));
	}

	@Test
	void testRemoveMessengerRoom() {
		boolean mesRoom = service.removeMessengerRoom("MSGR00000002");
		assertTrue(mesRoom);
	}

}
