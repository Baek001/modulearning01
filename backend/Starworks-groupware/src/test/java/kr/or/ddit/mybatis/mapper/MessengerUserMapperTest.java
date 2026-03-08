package kr.or.ddit.mybatis.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.vo.MessengerUserVO;

@Transactional
@SpringBootTest
class MessengerUserMapperTest {

	Logger log = LoggerFactory.getLogger(MessengerUserMapperTest.class);

	@Autowired
	MessengerUserMapper mapper;

	@Test
	void testInsertMessengerUser() {
		MessengerUserVO mesUser = new MessengerUserVO();
		mesUser.setUserId("2020010");
		mesUser.setMsgrId("MSGR00000002");
		/* mesUser.setJoinDt(LocalDateTime.now()); */
		int rowcnt = mapper.insertMessengerUser(mesUser);
		assertEquals(1, rowcnt);
	}

	@Test
	void testSelectMessengerUserList() {
		mapper.selectMessengerUserList().forEach(System.out::println);
	}

	@Test
	void testSelectMessengerUser() {
		MessengerUserVO mesUser = mapper.selectMessengerUser("2020004");
		assertNotNull(mesUser);
	}

	@Test
	void testDeleteMessengerUser() {
		assertEquals(1, mapper.deleteMessengerUser("2020004"));
	}

}
