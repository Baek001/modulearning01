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

import kr.or.ddit.vo.DepartmentScheduleVO;
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
class DepartmentScheduleMapperTest {
	@Autowired
	DepartmentScheduleMapper mapper;
	
	DepartmentScheduleVO vo;
	
	@BeforeEach
	void setUpBefore() {
		vo = new DepartmentScheduleVO();
		vo.setDeptId("DP001000");
		vo.setDeptSchdCrtUserId("a001");
		
		int res = mapper.insertDepartmentSchedule(vo); 
		assertTrue(res > 0);
	}
	
	@Test
	void testSelectDepartmentScheduleList() {
		List<DepartmentScheduleVO> list = mapper.selectDepartmentScheduleList();
		assertNotEquals(list, 0);
		log.info("Result : " + list);
	}

	@Test
	void testSelectDepartmentSchedule() {
		String insertId = vo.getDeptSchdId();
		DepartmentScheduleVO selectVo = mapper.selectDepartmentSchedule(insertId); 
		assertNotNull(selectVo);
		log.info("Result : " + selectVo);
	}

	@Test
	void testUpdateDepartmentSchedule() {
		vo.setDeptSchdExpln("수정된 TEST EXPLAIN");
		vo.setDelYn("Y");
		int res = mapper.updateDepartmentSchedule(vo);
		assertTrue(res > 0);
		String insertId = vo.getDeptSchdId(); 
		DepartmentScheduleVO selectVO = mapper.selectDepartmentSchedule(insertId);
		log.info("Result : " + selectVO);
	}

}
