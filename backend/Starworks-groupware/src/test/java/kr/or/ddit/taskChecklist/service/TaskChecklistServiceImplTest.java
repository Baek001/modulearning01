package kr.or.ddit.taskChecklist.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.task.checklist.service.TaskChecklistService;
import kr.or.ddit.vo.TaskChecklistVO;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author 김주민
 * @since 2025. 9. 26.
 * @see TaskChecklistServiceImplTest
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 26.     	김주민	          최초 생성
 *
 * </pre>
 */
@Slf4j
@SpringBootTest
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TaskChecklistServiceImplTest {
	
	@Autowired
	TaskChecklistService service;
	
	TaskChecklistVO taskChecklist;

	@Order(1)
	@Test
	void testCreateTaskChecklist() {
		TaskChecklistVO newTaskChecklist = new TaskChecklistVO();
		newTaskChecklist.setTaskId("BIZ999999999TASK9999");
		newTaskChecklist.setTaskPicId("c001");
		assertTrue(service.createTaskChecklist(newTaskChecklist));
	}

	@Order(2)
	@Test
	void testReadTaskChecklistList() {
		List<TaskChecklistVO> result = service.readTaskChecklistList("BIZ999999999TASK9999");
	    assertNotNull(result, "체크리스트 목록이 null이면 안됩니다");
	}

	@Order(3)
	@Test
	void testReadTaskChecklist() {
		Integer chkSqn = 5;
		TaskChecklistVO checklist = service.readTaskChecklist(chkSqn);
		assertNotNull(checklist);
		
		log.info("조회된 체크리스트 : {}", checklist);
	}
	
	@Order(4)
	@Test
	void testModifyTaskChecklist() {
		assertDoesNotThrow(() -> {
	        //유효한 ID
	        Integer chkSqn = 5;
	        TaskChecklistVO saved = service.readTaskChecklist(chkSqn);
	        
	        //조회된 객체가 null이 아닌지 확인
	        assertNotNull(saved, "테스트를 위한 메뉴 데이터가 존재하지 않습니다.");

	        saved.setChklistCont("수정된 더미테스트");
	        service.modifyTaskChecklist(saved);
	        
	        //수정된 내용을 다시 조회하여 검증
	        TaskChecklistVO updated = service.readTaskChecklist(saved.getChklistSqn());
	        assertEquals("수정된 더미테스트", updated.getChklistCont());
	    });
	}

	@Order(5)
	@Test
	void testRemoveTaskChecklist() {
		assertTrue(service.removeTaskChecklist(5));
	}

}
