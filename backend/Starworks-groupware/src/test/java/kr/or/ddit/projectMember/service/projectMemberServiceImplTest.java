package kr.or.ddit.projectMember.service;

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

import kr.or.ddit.project.member.service.projectMemberService;
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
class projectMemberServiceImplTest {
	@Autowired
	projectMemberService service;
	
	ProjectMemberVO projectMember;

	@Order(1)
	@Test
	void testCreateProjectMember() {
		ProjectMemberVO newProjectMember = new ProjectMemberVO();
		newProjectMember.setBizId("BIZ202599999");
		newProjectMember.setBizAuthCd("B102");
		newProjectMember.setBizUserId("c001");
		assertTrue(service.createProjectMember(newProjectMember));
	}

	@Order(2)
	@Test
	void testReadProjectMemberList() {
		assertNotEquals(0, service.readProjectMemberList("BIZ202599999"));
	}

	@Order(3)
	@Test
	void testReadProjectMember() {
	    ProjectMemberVO paramMember = new ProjectMemberVO();
	    paramMember.setBizId("BIZ202599999");
	    paramMember.setBizUserId("a001");

	    ProjectMemberVO member = service.readProjectMember(paramMember);

	    assertNotNull(member);
	    
	    log.info("조회된 프로젝트 인원: {}", member);
	}

	@Order(4)
	@Test
	void testModifyProjectMember() {
		 ProjectMemberVO member = new ProjectMemberVO();
	    member.setBizId("BIZ202599999");
	    member.setBizUserId("a001");

	    // 2. 수정할 값을 설정하고 서비스 메서드를 호출합니다.
	    member.setBizAuthCd("B102");
	    service.modifyProjectMember(member);

	    ProjectMemberVO updated = service.readProjectMember(member);

	    assertNotNull(updated, "수정 후 멤버를 다시 조회했는데 객체가 null입니다.");

	    assertEquals("B102", updated.getBizAuthCd(), "프로젝트 권한 코드가 올바르게 수정되지 않았습니다.");
		
	}

	@Order(5)
	@Test
	void testRemoveProjectMember() {
		ProjectMemberVO memberToRemove = new ProjectMemberVO();
	    memberToRemove.setBizId("BIZ202599999");
	    memberToRemove.setBizUserId("a001");

	    boolean result = service.removeProjectMember(memberToRemove);
	    assertTrue(result, "멤버 삭제 실패");

	    ProjectMemberVO deletedMember = service.readProjectMember(memberToRemove);

	    assertNotNull(deletedMember, "논리적 삭제 후 멤버를 다시 조회할 수 없습니다.");
	  
	    assertEquals("B103", deletedMember.getBizAuthCd());
	}
	
	@Order(6)
	@Test
	void testSelectProjectToProjectMember() {
	
	    String bizUserId = "a001";

	    List<ProjectMemberVO> bizId = service.readProjectList(bizUserId);

	    assertNotEquals(bizId.size(), 0);
	    
	    log.info("조회된 프로젝트 목록: {}", bizId);
	}
		


}
