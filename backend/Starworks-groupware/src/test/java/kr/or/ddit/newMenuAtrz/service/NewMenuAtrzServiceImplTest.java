package kr.or.ddit.newMenuAtrz.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.comm.exception.EntityNotFoundException;
import kr.or.ddit.menu.atrz.service.NewMenuAtrzService;
import kr.or.ddit.vo.NewMenuAtrzVO;

@Transactional
@SpringBootTest
class NewMenuAtrzServiceImplTest {

	@Autowired
	NewMenuAtrzService service;

	@Test
	void testReadNewMenuAtrzList() {
		fail("Not yet implemented");
	}

	@Test
	void testReadNewMenuAtrzNonPaging() {
		fail("Not yet implemented");
	}

	@Test
	void testReadNewMenuAtrz() {
		assertThrows(EntityNotFoundException.class, () -> service.readNewMenuAtrz(10));

		try {
			service.readNewMenuAtrz(10);
		} catch (EntityNotFoundException e) {
			System.out.println(e);
		}

		assertDoesNotThrow(() -> service.readNewMenuAtrz(2));
	}

	@Test
	void testCreateNewMenuAtrz() {
		NewMenuAtrzVO newMenuAtrz = new NewMenuAtrzVO();
		newMenuAtrz.setAtrzDocId("ATRZ000000000001");
		newMenuAtrz.setMenuNm("아이스아메리카노");
		newMenuAtrz.setMarketingContent("열심히 홍보하겟다");
		newMenuAtrz.setCostRatioAmt(99999);
		
		assertThrows(DataAccessException.class, () -> service.createNewMenuAtrz(newMenuAtrz));

//		try {
//			service.createNewMenuAtrz(newMenuAtrz);
//		} catch (OurBusinessException e) {
//			System.out.println(e);
//		}

	}


}
