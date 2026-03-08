package kr.or.ddit.vacation.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.vo.VactionVO;

@Transactional
@SpringBootTest
class VactionServiceImplTest {

	@Autowired
	VactionService service;
	
	@Test
	void testCreateVaction() {
		VactionVO vaction = new VactionVO();
		vaction.setVactSqn(15);
		vaction.setAtrzDocId("ATRZ000000000002");
		vaction.setVactUserId("c001");
		vaction.setVactCd("E203");
		vaction.setVactBgngDt(LocalDateTime.now());
		vaction.setVactEndDt(LocalDateTime.now());
		vaction.setUseVactCnt(1);
		assertTrue(service.createVaction(vaction));
	}

	@Test
	void testReadVactionList() {
		assertNotNull(service.readVactionList());
	}

	@Test
	void testReadVaction() {
		assertDoesNotThrow(()->service.readVaction("4"));
	}

}
