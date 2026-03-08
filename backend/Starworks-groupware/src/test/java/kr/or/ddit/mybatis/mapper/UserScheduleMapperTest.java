package kr.or.ddit.mybatis.mapper;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.vo.UserScheduleVO;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author 장어진
 * @since 2025. 9. 25.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 25.     	장어진	          최초 생성
 *
 * </pre>
 */
@Slf4j
@Transactional
@SpringBootTest
class UserScheduleMapperTest {

	@Autowired
	UserScheduleMapper mapper;
	
	UserScheduleVO vo;
	
	@BeforeEach
	void setUpBefore() {
		vo = new UserScheduleVO();
		vo.setUserId("c001");

		int res = mapper.insertUserSchedule(vo); 
		assertTrue(res > 0);
	}
	
	@Test
	void testSelectUserScheduleList() {
		List<UserScheduleVO> list = mapper.selectUserScheduleList();
		assertNotEquals(list, 0);
		log.info("Result : " + list);
	}
	
	@Test
	void testSelectUserSchedule() {
		String insertId = vo.getUserSchdId();
		UserScheduleVO selectVo = mapper.selectUserSchedule(insertId); 
		assertNotNull(selectVo);
		log.info("Result : " + selectVo);
	}
	
	@Test
	void testUpdateUserSchedule() {
		vo.setUserSchdExpln("수정된 TEST EXPLAIN");
		vo.setDelYn("Y");
		log.info("============> VO : {}", vo);
		int res = mapper.updateUserSchedule(vo);
		assertTrue(res > 0);
		String inserted = vo.getUserSchdId();
		UserScheduleVO selectVO = mapper.selectUserSchedule(inserted); 
		log.info("Result : " + selectVO);
	}
}
