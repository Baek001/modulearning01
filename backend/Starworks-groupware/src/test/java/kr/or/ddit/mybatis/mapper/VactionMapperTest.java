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

import kr.or.ddit.vo.VactionVO;

@Transactional
@SpringBootTest
class VactionMapperTest {

	Logger log = LoggerFactory.getLogger(VactionMapperTest.class);
	
	@Autowired
	VactionMapper mapper;
	
	@Test
	void testInsertVaction() {
		VactionVO vaction = new VactionVO();
		vaction.setVactSqn(15);
		vaction.setAtrzDocId("ATRZ000000000002");
		vaction.setVactUserId("c001");
		vaction.setVactCd("E203");
		vaction.setVactBgngDt(LocalDateTime.now());
		vaction.setVactEndDt(LocalDateTime.now());
		vaction.setUseVactCnt(1);
		int rowcnt = mapper.insertVaction(vaction);
		assertEquals(1, rowcnt);
	}

	@Test
	void testSelectVactionList() {
		mapper.selectVactionList().forEach(System.out::println);
	}

	@Test
	void testSelectVacation() {
		VactionVO vaction = mapper.selectVacation("4");
		assertNotNull(vaction);
	}

}
