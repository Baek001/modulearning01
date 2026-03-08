package kr.or.ddit.mybatis.mapper;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.vo.TimeAndAttendanceVO;
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
 *  2025. 9. 27.     	장어진	          LocalDate -> LocalDateTime 으로 변경
 *
 * </pre>
 */
@Slf4j
@Transactional
@SpringBootTest
class TimeAndAttendanceMapperTest {
	@Autowired
	TimeAndAttendanceMapper mapper;
	
	TimeAndAttendanceVO vo;
	
	@BeforeEach
	void setUpBefore() {
		vo = new TimeAndAttendanceVO();
		vo.setUserId("c001");

		int res = mapper.insertTimeAndAttendance(vo); 
		assertTrue(res > 0);		
	}
	
	@Test
	void testSelectTimeAndAttendanceList() {
		List<TimeAndAttendanceVO> list = mapper.selectTimeAndAttendanceList();
		assertNotEquals(list, 0);
		log.info("Result : " + list);
	}

	@Test
	void testSelectTimeAndAttendance() {
		String insertYMD = vo.getWorkYmd();
		String insertId = vo.getUserId();
		TimeAndAttendanceVO selectVo = mapper.selectTimeAndAttendance(insertId, insertYMD); 
		assertNotNull(selectVo);
		log.info("Result : " + selectVo);
	}
	
	@Test
	void testUpdateTimeAndAttendance() {
		vo.setWorkEndDt(LocalDateTime.now());
		int res = mapper.updateTimeAndAttendance(vo);
		assertTrue(res > 0);
		String insertedYMD = vo.getWorkYmd();
		String insertedId = vo.getUserId();
		TimeAndAttendanceVO selectVO = mapper.selectTimeAndAttendance(insertedId, insertedYMD); 
		log.info("Result : " + selectVO);
	}

}
