package kr.or.ddit.mybatis.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.vo.MessengerRoomVO;

@Transactional
@SpringBootTest
class MessengerRoomMapperTest {

	Logger log = LoggerFactory.getLogger(MessengerRoomMapperTest.class);
	
	@Autowired
	MessengerRoomMapper mapper;
	
	@Test
	void testInsertMessengerRoom() {
		MessengerRoomVO mesRoom = new MessengerRoomVO();
		mesRoom.setMsgrId("MSGR00000011");
		mesRoom.setMsgrNm("TEST ROOM");
		int rowcnt = mapper.insertMessengerRoom(mesRoom);
		assertEquals(1, rowcnt);
	}

	@Test
	void testSelectMessengerRoomList() {
		mapper.selectMessengerRoomList().forEach(System.out::println);
	}

	@Test
	void testSelectMessengerRoom() {
		MessengerRoomVO mesRoom = mapper.selectMessengerRoom("MSGR00000005");
		assertNotNull(mesRoom);
	}

	@Test
	void testDeleteMessengerRoom() {
		assertEquals(1, mapper.deleteMessengerRoom("MSGR00000005"));
	}

}
