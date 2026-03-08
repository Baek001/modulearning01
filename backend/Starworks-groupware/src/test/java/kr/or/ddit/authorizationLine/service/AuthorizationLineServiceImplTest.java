package kr.or.ddit.authorizationLine.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.approval.line.service.AuthorizationLineService;
import kr.or.ddit.vo.AuthorizationLineVO;

@Transactional
@SpringBootTest
class AuthorizationLineServiceImplTest {
	
	@Autowired
	AuthorizationLineService service;
	
	@Test
	void testCreateAuthorizationLine() {
		AuthorizationLineVO authLine = new AuthorizationLineVO();
		authLine.setAtrzDocId("ATRZ000000000005");
		authLine.setAtrzApprUserId("a001");
		assertTrue(service.createAuthorizationLine(authLine));
	}

}
