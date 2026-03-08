package kr.or.ddit.mybatis.mapper;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.vo.MainTaskVO;
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
class MainTaskMapperTest {
	
	@Autowired
	MainTaskMapper mapper;
	MainTaskVO mainTask;

	@BeforeEach
	void setUpBefore() {
		mainTask = new MainTaskVO();
		mainTask.setTaskId("BIZ111111111TASK1111");
		mainTask.setBizId("BIZ202599999");
		mainTask.setTaskNm("TEST2");
		mainTask.setTaskSttsCd("B401");
		mainTask.setBizUserId("a001");
	}

	@Order(1)
	@Test
	void testInsertMainTask() {
		assertDoesNotThrow(()->{
			int rowcnt = mapper.insertMainTask(mainTask);
			assertEquals(1, rowcnt);
			
			assertNotNull(mainTask.getTaskId());
			log.info("삽입된 주요 업무: {}", mainTask);
		});
	}

	@Order(2)
	@Test
	void testSelectMainTaskListNonPaging() {
		assertDoesNotThrow(()->mapper.selectMainTaskListNonPaging("BIZ202599999"));
	}

	@Order(3)
	@Test
	void testSelectMainTask() {
		// 먼저 데이터 삽입
	    mapper.insertMainTask(mainTask);
	    
	    MainTaskVO result = mapper.selectMainTask(mainTask.getTaskId());
	    assertNotNull(result);
	    log.info("조회된 주요업무: {}", result);
	}

	@Order(4)
	@Test
	void testUpdateMainTask() {
		// 먼저 데이터 삽입
	    mapper.insertMainTask(mainTask);
	    
	    MainTaskVO result = mapper.selectMainTask(mainTask.getTaskId());
	    result.setTaskNm("TEST 수정");
	    assertEquals(1, mapper.updateMainTask(result));
	    
	    log.info("수정된 주요업무 : {}", result);
	}

	@Order(5)
	@Test
	void testDeleteMainTask() {
		// 먼저 데이터 삽입
	    mapper.insertMainTask(mainTask);
	    
	    int result = mapper.deleteMainTask("BIZ999999999TASK9999");
	    assertEquals(1, result, "삭제 업데이트가 실행되지 않았습니다.");
	    
	    MainTaskVO afterDelete = mapper.selectMainTask(mainTask.getTaskId());
	    assertNotNull(afterDelete, "논리적 삭제이므로 데이터는 여전히 존재해야 합니다");
	    assertEquals("B405", afterDelete.getTaskSttsCd(), "상태코드가 삭제 상태로 변경되지 않았습니다");
	}

}
