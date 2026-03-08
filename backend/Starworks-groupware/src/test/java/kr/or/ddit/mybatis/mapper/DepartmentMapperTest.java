package kr.or.ddit.mybatis.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.vo.DepartmentVO;

@Transactional
@SpringBootTest
class DepartmentMapperTest {

	Logger log = LoggerFactory.getLogger(DepartmentMapperTest.class);
	
	@Autowired
	DepartmentMapper mapper;
	
	@BeforeEach
	void testInsertDepartment() {
		DepartmentVO dept = new DepartmentVO();
		dept.setDeptId("DP004009");
		dept.setDeptNm("브랜드전략팀");
		int rowcnt = mapper.insertDepartment(dept);
		assertEquals(1, rowcnt);
	}

	@Test
	void testSelectDepartmentList() {
		mapper.selectDepartmentList().forEach(System.out::println);
	}

	@Test
	void testSelectDepartment() {
		DepartmentVO dept = mapper.selectDepartment("DP004001");
		assertNotNull(dept);
	}

	@Test
	void testDeleteDepartment() {
		assertEquals(1, mapper.deleteDepartment("DP004001"));
	}

}
