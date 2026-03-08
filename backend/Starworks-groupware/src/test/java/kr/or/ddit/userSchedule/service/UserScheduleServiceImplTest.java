package kr.or.ddit.userSchedule.service;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.calendar.users.service.UserScheduleService;
import kr.or.ddit.comm.exception.EntityNotFoundException;
import kr.or.ddit.vo.UserScheduleVO;
import lombok.extern.slf4j.Slf4j;
/**
 * 
 * @author 장어진
 * @since 2025. 9. 26.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 26.     	장어진	          최초 생성
 *
 * </pre>
 */
@Slf4j
@Transactional
@SpringBootTest
class UserScheduleServiceImplTest {
	
	@Autowired
	UserScheduleService service;

	UserScheduleVO vo;

	@BeforeEach
	void setBefore() {
		vo = new UserScheduleVO();
		vo.setUserId("c001");
		
		assertNotEquals(0, service.createUserSchedule(vo));
		log.info("Result CREATE : {}", vo);
	}
	@Test
	void testReadUserScheduleList() {
		List<UserScheduleVO> list = service.readUserScheduleList();

		log.info("Result READ LIST : {}", list);
		assertNotEquals(0, list.size());
	}

	@Test
	void testReadUserSchedule() {
		UserScheduleVO insertVO = service.readUserSchedule(vo.getUserSchdId());

		log.info("Result READ ONE : {}", insertVO);
		assertNotNull(insertVO);

		assertThrows(EntityNotFoundException.class, () -> 
			service.readUserSchedule("aaaa")
		);
	}

	@Test
	void testModifyUserSchedule() {
		vo.setUserSchdExpln("수정된 TEST EXPLAIN");
		vo.setDelYn("Y");

		assertNotEquals(0, service.modifyUserSchedule(vo));
		log.info("Result MODIFY : {}", vo);
	}

}
