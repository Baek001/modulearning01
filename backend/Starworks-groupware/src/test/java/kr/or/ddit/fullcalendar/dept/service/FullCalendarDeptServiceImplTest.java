package kr.or.ddit.fullcalendar.dept.service;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.calendar.dept.service.FullCalendarDeptService;
import kr.or.ddit.dto.FullCalendarDeptDTO;
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
 *  2025. 9. 27.     	장어진	          @Param FullCalendarDeptDTO로 수정
 *
 * </pre>
 */
@Slf4j
@Transactional
@SpringBootTest
class FullCalendarDeptServiceImplTest {
	
	@Autowired
	FullCalendarDeptService service;
	
	FullCalendarDeptDTO dto;
	
	@Test
	void testSelectFullCalendarDeptList() {
		dto = new FullCalendarDeptDTO();
		dto.setUserId("a001");
		dto.setDeptId("DP001001");
		
		List<FullCalendarDeptDTO> list = service.readFullCalendarDeptList(dto);
		
		log.info("Result READ LIST : {}", list);
		assertNotEquals(0, list.size());
	}

}
