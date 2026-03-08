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

import kr.or.ddit.vo.BusinessTripVO;

@Transactional
@SpringBootTest
class BusinessTripMapperTest {
	Logger log = LoggerFactory.getLogger(BusinessTripMapperTest.class);
	
	@Autowired
	BusinessTripMapper mapper;
	
	@Test
	void testInsertBusinessTrip() {
		BusinessTripVO busTrip = new BusinessTripVO();
		busTrip.setBztrSqn(21);
		busTrip.setAtrzDocId("ATRZ000000000002");
		busTrip.setBztrUserId("c001");
		busTrip.setBztrCd("E203");
		busTrip.setBztrBgngDt(LocalDateTime.now());
		busTrip.setBztrEndDt(LocalDateTime.now());
		int rowcnt = mapper.insertBusinessTrip(busTrip);
		assertEquals(1, rowcnt);
	}

	@Test
	void testSelectBusinessTripList() {
		mapper.selectBusinessTripList().forEach(System.out::println);
	}

	@Test
	void testSelectBusinessTrip() {
		BusinessTripVO busTrip = mapper.selectBusinessTrip("17");
		assertNotNull(busTrip);
	}

}
