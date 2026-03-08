package kr.or.ddit.users.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.vo.UsersVO;

@Transactional
@SpringBootTest
class UsersServiceImplTest {

	@Autowired
	UsersService service;
	
	@Test
	void testCreateUser() {
		UsersVO user = new UsersVO();
		user.setUserId("b001");
		user.setUserPswd("java");
		user.setUserNm("윤서현");
		user.setUserEmail("asds@starwork.com");
		user.setDeptId("DP001002");
		user.setJbgdCd("JBGD05");
		assertTrue(service.createUser(user));
	}

	@Test
	void testReadUserList() {
		assertNotNull(service.readUserList());
	}

	@Test
	void testReadUser() {
		assertDoesNotThrow(()->service.readUser("a001"));
	}

	@Test
	void testModifyUser() {
		UsersVO user = service.readUser("a001");
		user.setUserNm("이름수정");
		assertTrue(service.modifyUser(user));
	}

	@Test
	void testRetireUser() {
		boolean user = service.retireUser("a001");
		assertTrue(user);;
		
	}

}
