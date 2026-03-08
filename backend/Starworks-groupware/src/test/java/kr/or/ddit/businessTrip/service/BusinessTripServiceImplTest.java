package kr.or.ddit.businessTrip.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.businesstrip.service.BusinessTripService;
import kr.or.ddit.vo.BusinessTripVO;

@Transactional
@SpringBootTest
class BusinessTripServiceImplTest {

	@Autowired
	BusinessTripService service;
	
	@Test
	void testCreateBusinessTrip() {
		BusinessTripVO busTrip = new BusinessTripVO();
		busTrip.setBztrSqn(21);
		busTrip.setAtrzDocId("ATRZ000000000002");
		busTrip.setBztrUserId("c001");
		busTrip.setBztrCd("E203");
		busTrip.setBztrBgngDt(LocalDateTime.now());
		busTrip.setBztrEndDt(LocalDateTime.now());
		assertTrue(service.createBusinessTrip(busTrip));
	}

	@Test
	void testReadBusinessTripList() {
		assertNotNull(service.readBusinessTripList());
	}

	@Test
	void testReadBusinessTrip() {
		assertDoesNotThrow(()->service.readBusinessTrip("17"));
	}

}
