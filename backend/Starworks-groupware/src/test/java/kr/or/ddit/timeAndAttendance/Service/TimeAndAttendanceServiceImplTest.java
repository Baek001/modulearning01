package kr.or.ddit.timeAndAttendance.Service;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.attendance.service.TimeAndAttendanceService;
import kr.or.ddit.comm.exception.EntityNotFoundException;
import kr.or.ddit.vo.TimeAndAttendanceVO;
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
 *  2025. 9. 27.     	장어진	          LocalDate -> LocalDateTime 으로 변경
 *
 * </pre>
 */
@Slf4j
@Transactional
@SpringBootTest
class TimeAndAttendanceServiceImplTest {

	@Autowired
	TimeAndAttendanceService service;

	TimeAndAttendanceVO vo;

//	@BeforeEach
//	void setBefore() {
//		vo = new TimeAndAttendanceVO();
//		vo.setUserId("c001");
//
//		assertNotEquals(0, service.createTimeAndAttendance(vo));
//		log.info("Result CREATE : {}", vo);
//	}

	@Test
	void testReadTimeAndAttendanceList() {
		List<TimeAndAttendanceVO> list = service.readTimeAndAttendanceList();

		log.info("Result READ LIST : {}", list);
		assertNotEquals(0, list.size());
	}

	@Test
	void testReadTimeAndAttendance() {
		TimeAndAttendanceVO insertVO = service.readTimeAndAttendance(vo.getUserId(), vo.getWorkYmd());

		log.info("Result READ ONE : {}", insertVO);
		assertNotNull(insertVO);

		assertThrows(EntityNotFoundException.class, () ->
			service.readTimeAndAttendance("aaaa", vo.getWorkYmd())
		);
	}

	@Test
	void testModifyTimeAndAttendance() {
		vo.setWorkEndDt(LocalDateTime.now());

		assertNotEquals(0, service.modifyTimeAndAttendance(vo));
		log.info("Result MODIFY : {}", vo);
	}

}
