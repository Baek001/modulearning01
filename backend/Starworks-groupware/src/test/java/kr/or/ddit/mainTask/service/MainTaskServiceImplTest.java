package kr.or.ddit.mainTask.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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

import kr.or.ddit.task.main.service.MainTaskService;
import kr.or.ddit.vo.MainTaskVO;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author 김주민
 * @since 2025. 9. 26.
 * @see MainTaskServiceImplTest
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
class MainTaskServiceImplTest {
	
	@Autowired
	MainTaskService service;
	
	MainTaskVO mainTask;

	@Order(1)
	@Test
	void testCreateMainTask() {
		MainTaskVO newMainTask = new MainTaskVO();
		newMainTask.setTaskId("BIZ111111111TASK1111");
		newMainTask.setBizId("BIZ202599999");
		newMainTask.setTaskNm("TEST2");
		newMainTask.setTaskSttsCd("B401");
		newMainTask.setBizUserId("a001");
		assertTrue(service.createMainTask(newMainTask));
	}

	@Order(2)
	@Test
	void testReadMainTaskListNonPaging() {
	    String bizId = "BIZ202599999"; 
	    List<MainTaskVO> mainTaskList = service.readMainTaskListNonPaging(bizId);

	    assertNotEquals(0, mainTaskList.size());
	}

	@Order(3)
	@Test
	void testReadMainTask() {
		String taskId = "BIZ999999999TASK9999";
		MainTaskVO mainTask = service.readMainTask(taskId);
		assertNotNull(mainTask);
		
		log.info("조회된 주요 업무 : {}", mainTask);
		log.info("조회된 주요 업무 상세 : {}", mainTask.getTaskDetail());
		
	}

	@Order(4)
	@Test
	void testModifyMainTask() {
		assertDoesNotThrow(() -> {
	        String taskId = "BIZ999999999TASK9999";
	        MainTaskVO saved = service.readMainTask(taskId);
	       
	        assertNotNull(saved);

	        saved.setTaskDetail("수정된 업무 상세설명");
	        service.modifyMainTask(saved);

	        MainTaskVO updated = service.readMainTask(saved.getTaskId());
	        assertEquals("수정된 업무 상세설명", updated.getTaskDetail());
	    });
	}

	@Order(5)
	@Test
	void testRemoveMainTask() {
		assertTrue(service.removeMainTask("BIZ999999999TASK9999"));
	}

}
