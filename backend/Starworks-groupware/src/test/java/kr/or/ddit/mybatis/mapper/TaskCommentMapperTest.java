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

import kr.or.ddit.vo.TaskCommentVO;
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
class TaskCommentMapperTest {
	@Autowired
	TaskCommentMapper mapper;
	TaskCommentVO taskComment;
	
	@BeforeEach
	void setUpBefore() {
		taskComment = new TaskCommentVO();
		taskComment.setTaskId("BIZ202599999TASK0001");
		taskComment.setContents("더미데이터 등록1");              
		taskComment.setCrtUserId("a001");
 	}

	@Order(1)
	@Test
	void testInsertTaskComment() {
		 assertDoesNotThrow(() -> {
		        int result = mapper.insertTaskComment(taskComment);
		        assertEquals(1, result, "코멘트 등록 실패");
		        
		        log.info("삽입된 코멘트: {}", taskComment);
		    });
	}

	@Order(2)
	@Test
	void testSelectTaskCommentList() {
		assertDoesNotThrow(()-> mapper.selectTaskCommentList("BIZ202599999TASK0001"));	
	}

	@Order(3)
	@Test
	void testSelectTaskComment() {
		taskComment = new TaskCommentVO();
		TaskCommentVO result = mapper.selectTaskComment(taskComment.getTaskCommSqn());
	}

	@Order(4)
	@Test
	void testDeleteTaskComment() {
		assertEquals(1, mapper.deleteTaskComment(5));
	}

}
