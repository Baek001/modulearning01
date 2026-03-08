package kr.or.ddit.mybatis.mapper;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.vo.ProjectVO;
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
class ProjectMapperTest {
	@Autowired
	ProjectMapper mapper;
	
	ProjectVO project;

	@Order(1)
	@Test
	void testInsertProject() {
		ProjectVO project = new ProjectVO();
		project.setBizId("AIZ202511111");
		project.setBizNm("TEST2");
		project.setBizPicId("c001");
		project.setBizTypeCd("B201");
		project.setBizSttsCd("B301");
		assertDoesNotThrow(()->{
			int rowcnt = mapper.insertProject(project);
			assertEquals(1, rowcnt);
		});
	}

	@Order(2)
	@Test
	void testSelectProjectListNonPaging() {
		List<ProjectVO> project = mapper.selectProjectListNonPaging();
		assertDoesNotThrow(()->project);
		log.info("조회된 프로젝트 : {}",project);
	}

	@Order(3)
	@Test
	void testSelectProject() {
		String bizId = "BIZ202599999";
		ProjectVO project = mapper.selectProject(bizId);
		assertNotNull(project);
		
		log.info("조회된 프로젝트 : {}", project);
		log.info("조회된 프로젝트 일련번호 : {}", project.getBizId());
	}

	@Order(4)
	@Test
	void testUpdateProject() {
		ProjectVO project = mapper.selectProject("BIZ202599999");
		project.setBizNm("두번째TEST");
		assertEquals(1, mapper.updateProject(project));
	}
	
	@Order(5)
	@Test
	void testDeleteProject() {
		String targetId = "BIZ202599999";
		// 논리적 삭제 실행
	    int result = mapper.deleteProject(targetId);
	    assertEquals(1, result, "삭제 업데이트가 실행되지 않았습니다");
	    
	    // 삭제 후 상태 확인
	    ProjectVO afterDelete = mapper.selectProject(targetId);
	    assertNotNull(afterDelete, "논리적 삭제이므로 데이터는 여전히 존재해야 합니다");
	    assertEquals("B405", afterDelete.getBizSttsCd(), "상태코드가 삭제 상태로 변경되지 않았습니다");
	}

}
