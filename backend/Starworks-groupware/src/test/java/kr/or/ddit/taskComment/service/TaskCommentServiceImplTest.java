package kr.or.ddit.taskComment.service;

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

import kr.or.ddit.task.comment.service.TaskCommentService;
import kr.or.ddit.vo.TaskCommentVO;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author 김주민
 * @since 2025. 9. 26.
 * @see TaskCommentServiceImplTest
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
class TaskCommentServiceImplTest {
	
	@Autowired
	TaskCommentService service;
	
	TaskCommentVO taskComment;

	@Order(1)
	@Test
	void testCreateTaskComment() {
		TaskCommentVO newTaskComment = new TaskCommentVO();
		newTaskComment.setTaskId("BIZ202599999TASK0001");
		newTaskComment.setCrtUserId("2020031");
		assertTrue(service.createTaskComment(newTaskComment));
		
	}

	@Order(2)
	@Test
	void testReadTaskCommentList() {
		List<TaskCommentVO> result = service.readTaskCommentList("BIZ999999999TASK9999");
		assertNotNull(result, "체크리스트 목록이 null이면 안됩니다");
	}

	@Order(3)
	@Test
	void testReadTaskComment() {
		Integer tackCommentSqn = 4;
		TaskCommentVO taskComment = service.readTaskComment(tackCommentSqn);
		assertNotNull(taskComment);
		
		log.info("조회된 코멘트 : {}",taskComment);
		
	}

	@Order(4)
	@Test
	void testRemoveTaskComment() {
		assertTrue(service.removeTaskComment(4));
	}

}
