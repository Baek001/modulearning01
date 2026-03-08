package kr.or.ddit.jobGrade.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.jobgrade.service.JobGradeService;
@Transactional
@SpringBootTest
class JobGradeServiceImplTest {

	@Autowired
	JobGradeService service;
	
	@Test
	void testReadJobGradeList() {
		assertNotNull(service.readJobGradeList());
	}

	@Test
	void testReadJobGrade() {
		assertDoesNotThrow(()->service.readJobGrade("JBGD02"));
	}

}
