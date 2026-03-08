package kr.or.ddit.project.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.project.mngt.service.projectService;
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
 *  2025. 9. 29.		김주민			  testCreateProject 수정
 *
 * </pre>
 */
@Slf4j
@SpringBootTest
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class projectServiceImplTest {
	
	@Autowired
	projectService service;
	
	ProjectVO project;

	@Order(1)
	@Test
	void testCreateProject() {
		ProjectVO newProject = new ProjectVO();
		newProject.setBizNm("더미테스트");
		newProject.setBizPicId("a001");
		newProject.setBizTypeCd("B201");
		newProject.setBizSttsCd("B301");
		
		String createdBizId = service.createProject(newProject);
		
		assertNotNull(createdBizId, "프로젝트 등록에 성공해야 합니다.");
	}

	@Order(2)
	@Test
	void testReadProjectListNonPaging() {
		assertNotEquals(0, service.readProjectListNonPaging());
	}

	@Order(3)
	@Test
	void testReadProject() {
		String bizId = "BIZ202599999";
		ProjectVO project = service.readProject(bizId);
		assertNotNull(project);
		
		log.info("조회된 프로젝트: {}", project);
	}

	@Order(4)
	@Test
	void testModifyProject() {
		assertDoesNotThrow(() -> {
	        //유효한 ID
	        String bizId = "BIZ202599999";
	        ProjectVO saved = service.readProject(bizId);
	        
	        //조회된 객체가 null이 아닌지 확인
	        assertNotNull(saved, "테스트를 위한 메뉴 데이터가 존재하지 않습니다.");

	        saved.setBizNm("수정된 프로젝트명");
	        service.modifyProject(saved);
	        
	        //수정된 내용을 다시 조회하여 검증
	        ProjectVO updated = service.readProject(saved.getBizId());
	        assertEquals("수정된 프로젝트명", updated.getBizNm());
	    });
	}

	@Order(5)
	@Test
	void testRemoveProject() {
		assertTrue(service.removeProject("BIZ202599999"));
	}

}
