package kr.or.ddit.mybatis.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.vo.UsersVO;

/**
 * 
 * @author 홍현택
 * @since 2025. 9. 27.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 27.     	홍현택	          로그인 설정을 위한 주석처리
 *
 * </pre>
 */
@Transactional
@SpringBootTest
class UsersMapperTest {

	Logger log = LoggerFactory.getLogger(UsersMapperTest.class);
	
	@Autowired
	UsersMapper mapper;
	
	@BeforeEach
	void testInsertUser() {
		UsersVO user = new UsersVO();
		user.setUserId("2025025");
		user.setUserPswd("java");
		user.setUserNm("윤서현");
		user.setUserEmail("asds@starwork.com");
		user.setDeptId("DP001002");
		user.setJbgdCd("JBGD05");
		int rowcnt = mapper.insertUser(user);
		assertEquals(1, rowcnt);
	}

	@Test
	void testSelectUserList() {
		mapper.selectUserList().forEach(System.out::println);
	}

	@Test
	void testSelectUser() {
		UsersVO user = mapper.selectUser("a001");
		assertNotNull(user);
	}

	@Test
	void testUpdateUser() {
		UsersVO user = mapper.selectUser("a001");
		user.setUserNm("서현");
		assertEquals(1, mapper.updateUser(user));
	}

//	@Test
//	void testRetireUser() {
//		UsersVO user = mapper.selectUser("a001");
//		user.setRsgntnYn("Y");
//		assertEquals(1, mapper.updateUser(user));
//	}

}
