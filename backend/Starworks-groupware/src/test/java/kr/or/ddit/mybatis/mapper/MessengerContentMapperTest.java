package kr.or.ddit.mybatis.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.vo.MessengerContentVO;

@Transactional
@SpringBootTest
class MessengerContentMapperTest {

	Logger log = LoggerFactory.getLogger(MessengerContentMapperTest.class);
	
	@Autowired
	MessengerContentMapper mapper;
	
	@BeforeEach
	void testInsertMessengerContent() {
		MessengerContentVO mesContent = new MessengerContentVO();
		mesContent.setMsgContId("MSG00000000012");
		mesContent.setUserId("2020005");
		mesContent.setMsgrId("MSGR00000002");
		int rowcnt = mapper.insertMessengerContent(mesContent);
		assertEquals(1, rowcnt);
	}

	@Test
	void testSelectMessengerContentList() {
		mapper.selectMessengerContentList().forEach(System.out::println);
	}

	@Test
	void testDeleteMessengerContent() {
		assertEquals(1, mapper.deleteMessengerContent("MSG00000000008"));
	}

}
