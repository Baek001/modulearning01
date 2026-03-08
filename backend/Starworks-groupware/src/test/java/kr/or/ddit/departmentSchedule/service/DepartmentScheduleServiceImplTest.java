package kr.or.ddit.departmentSchedule.service;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.calendar.front.service.DepartmentScheduleService;
import kr.or.ddit.comm.exception.EntityNotFoundException;
import kr.or.ddit.vo.DepartmentScheduleVO;
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
class DepartmentScheduleServiceImplTest {

	@Autowired
	DepartmentScheduleService service;
	
	DepartmentScheduleVO vo;
	
	@BeforeEach
	void setBefore() {
		vo = new DepartmentScheduleVO();
		vo.setDeptId("DP001000");
		vo.setDeptSchdCrtUserId("a001");
		
		assertNotEquals(false, service.createDepartmentSchedule(vo));
		log.info("Result CREATE : {}", vo);
	}
	
	@Test
	void testReadDepartmentScheduleList() {
		List<DepartmentScheduleVO> list = service.readDepartmentScheduleList();
		
		log.info("Result READ LIST : {}", list);
		assertNotEquals(0, list.size());
	}

	@Test
	void testReadDepartmentSchedule() {
		DepartmentScheduleVO insertVO = service.readDepartmentSchedule(vo.getDeptSchdId());
		
		log.info("Result READ ONE : {}", insertVO);
		assertNotNull(insertVO);
		
		assertThrows(EntityNotFoundException.class, () -> 
			service.readDepartmentSchedule("aaaa")
		);
	}

	@Test
	void testModifyDepartmentSchedule() {
		vo.setDeptSchdExpln("수정된 TEST EXPLAIN");
		vo.setDelYn("Y");
		
		assertNotEquals(false, service.modifyDepartmentSchedule(vo));
		log.info("Result MODIFY : {}", vo);		
	}

}
