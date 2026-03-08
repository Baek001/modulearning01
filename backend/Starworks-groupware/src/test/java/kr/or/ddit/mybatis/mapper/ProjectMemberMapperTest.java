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

import kr.or.ddit.vo.ProjectMemberVO;
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
class ProjectMemberMapperTest {
	@Autowired
	ProjectMemberMapper mapper;
	ProjectMemberVO member;
	String bizId;

	@BeforeEach
	void setUpBefore() {
		member = new ProjectMemberVO();
		member.setBizId("BIZ202599999");
		member.setBizAuthCd("B102");
		member.setBizUserId("c001");
	}
	
	@Order(1)
	@Test
	void testInsertProjectMember() {
		assertDoesNotThrow(()->{
			int rowcnt = mapper.insertProjectMember(member);
			assertEquals(1, rowcnt);
			
			assertNotNull(member.getBizId());
			log.info("생성된 프로젝트 ID: {}", bizId);
			log.info("삽입된 멤버: {}", member);
		});
	}

	@Order(2)
	@Test
	void testSelectProjectMemberByProject() {
		assertDoesNotThrow(()->mapper.selectProjectMemberByProject(bizId));
	}

	@Order(3)
	@Test
	void testSelectProjectMember() {
		ProjectMemberVO projectMember = new ProjectMemberVO();
		projectMember.setBizId("BIZ202599999");
		projectMember.setBizUserId("a001");
		ProjectMemberVO result = mapper.selectProjectMember(projectMember);
		
		assertNotNull(result);
		log.info("조회된 프로젝트 멤버 조회 : {}", result);
	}

	@Order(4)
	@Test
	void testUpdateProjectMember() {
		mapper.insertProjectMember(member);
		
		ProjectMemberVO projectMember = mapper.selectProjectMember(member);
		projectMember.setBizAuthCd("B103");
		assertEquals(1, mapper.updateProjectMember(projectMember));
	}

	@Order(5)
	@Test
	void testDeleteProjectMember() {
		// 데이터 삽입
	    mapper.insertProjectMember(member);
		
		//논리적 삭제 실행
		int result = mapper.deleteProjectMember(member);
		assertEquals(1, result, "삭제 업데이트가 실행되지 않았습니다.");
		
		ProjectMemberVO afterDelete = mapper.selectProjectMember(member);
		 assertNotNull(afterDelete, "논리적 삭제이므로 데이터는 여전히 존재해야 합니다");
		 assertEquals("B104", afterDelete.getBizAuthCd(), "상태코드가 삭제 상태로 변경되지 않았습니다");
	}
	
	@Order(6)
	@Test
	void testSelectProjectByProjectMember() {
		assertDoesNotThrow(()->mapper.selectProjectByProjectMember(member.getBizUserId()));
	}

}
