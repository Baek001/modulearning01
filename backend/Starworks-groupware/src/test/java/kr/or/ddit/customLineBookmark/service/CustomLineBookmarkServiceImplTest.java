package kr.or.ddit.customLineBookmark.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.approval.bookmark.service.CustomLineBookmarkService;
import kr.or.ddit.vo.CustomLineBookmarkVO;

@Transactional
@SpringBootTest
class CustomLineBookmarkServiceImplTest {

	@Autowired
	CustomLineBookmarkService service;
	
	@Test
	void testCreateCustomLineBookmark() {
		CustomLineBookmarkVO custLine = new CustomLineBookmarkVO();
		custLine.setUserId("a001");
		assertTrue(service.createCustomLineBookmark(custLine));
	}

	/*@Test
	void testReadCustomLineBookmarkList() {
		assertNotNull(service.readCustomLineBookmarkList());
	}*/

	@Test
	void testRemoveCustomLineBookmark() {
		service.removeCustomLineBookmark("a001", "sample-bookmark");
		assertTrue(true);
	}

}
