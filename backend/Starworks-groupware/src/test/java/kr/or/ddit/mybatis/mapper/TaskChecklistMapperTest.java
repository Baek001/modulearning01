package kr.or.ddit.mybatis.mapper;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.vo.TaskChecklistVO;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author 김주민
 * @since 2025. 9. 25.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 25.     	김주민	          최초 생성
 *
 * </pre>
 */
@Slf4j
@SpringBootTest
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TaskChecklistMapperTest {
	@Autowired
	TaskChecklistMapper mapper;
	
	TaskChecklistVO taskChecklist;
	
	@BeforeEach
	void setUpBefore() {
		taskChecklist = new TaskChecklistVO();
		taskChecklist.setTaskId("BIZ999999999TASK9999");
	    taskChecklist.setTaskPicId("a001");               
	    taskChecklist.setChklistCont("테스트 체크리스트");
	    taskChecklist.setCompltYn("N");   
	}

	@Order(1)
	@Test
	void testInsertTaskChecklist() {	    
	    assertDoesNotThrow(() -> {
	        int result = mapper.insertTaskChecklist(taskChecklist);
	        assertEquals(1, result, "체크리스트 삽입 실패");
	        
	        log.info("삽입된 체크리스트: {}", taskChecklist);
	    });
	}

	@Order(2)
	@Test
	void testSelectTaskChecklistList() {
		assertDoesNotThrow(()-> mapper.selectTaskChecklistList("BIZ999999999TASK9999"));
	}

	@Order(3)
	@Test
	void testUpdateTaskChecklist() {
		mapper.insertTaskChecklist(taskChecklist);
		
		TaskChecklistVO result = mapper.selectTaskChecklist(taskChecklist.getChklistSqn());
		result.setChklistCont("TEST수정수정");
		assertEquals(1, mapper.updateTaskChecklist(result));
	    
	    log.info("수정된 업무 체크리스트 : {}", result);
	}

	@Order(4)
	@Test
	void testDeleteTaskChecklist() {
		assertEquals(1, mapper.deleteTaskChecklist(5));
	    
	}

}
