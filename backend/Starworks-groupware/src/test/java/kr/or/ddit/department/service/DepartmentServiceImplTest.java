package kr.or.ddit.department.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.department.service.DepartmentService;
import kr.or.ddit.vo.DepartmentVO;

@Transactional
@SpringBootTest
class DepartmentServiceImplTest {

	@Autowired
	DepartmentService service;
	
	@Test
	void testCreateDepartment() {
		DepartmentVO dept = new DepartmentVO();
		dept.setDeptId("DP004009");
		dept.setDeptNm("브랜드전략팀");
		assertTrue(service.createDepartment(dept));
	}

	@Test
	void testReadDepartmentList() {
		assertNotNull(service.readDepartmentList());
	}

	@Test
	void testReadDepartment() {
		assertDoesNotThrow(()->service.readDepartment("DP004000"));
	}

	@Test
	void testRemoveDepartment() {
		boolean dept = service.removeDepartment("DP004000");
		assertTrue(dept);
	}

}
