package kr.or.ddit.mybatis.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.vo.AuthorizationLineVO;

@Transactional
@SpringBootTest
class AuthorizationLineMapperTest {

	Logger log = LoggerFactory.getLogger(AuthorizationLineMapperTest.class);
	
	@Autowired
	AuthorizationLineMapper mapper;
	
	@Test
	void testInsertAuthLine() {
		AuthorizationLineVO AuthLine = new AuthorizationLineVO();
		AuthLine.setAtrzDocId("ATRZ000000000005");
		AuthLine.setAtrzApprUserId("a001");
		int rowcnt = mapper.insertAuthLine(AuthLine);
		assertEquals(1, rowcnt);
	}

}
