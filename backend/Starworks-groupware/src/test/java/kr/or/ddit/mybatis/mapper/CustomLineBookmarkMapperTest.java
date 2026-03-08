package kr.or.ddit.mybatis.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@SpringBootTest
class CustomLineBookmarkMapperTest {

	Logger log = LoggerFactory.getLogger(CustomLineBookmarkMapperTest.class);
	
	@Autowired
	CustomLineBookmarkMapper mapper;
	
	/*@BeforeEach
	void testInsertCustLineBookmark() {
		CustomLineBookmarkVO custLine = new CustomLineBookmarkVO();
		custLine.setUserId("a001");
		int rowcnt = mapper.insertCustLineBookmark(custLine);
		assertEquals(1, rowcnt);
	}

	@Test
	void testSelectCustLineBookmarkList() {
		mapper.selectCustLineBookmarkList().forEach(System.out::println);
	}*/

	@Test
	void testDeleteCustomLineBookmark() {
		mapper.deleteCustomLineBookmark("a001", "sample-bookmark");
		assertEquals(1, 1);
	}

}
