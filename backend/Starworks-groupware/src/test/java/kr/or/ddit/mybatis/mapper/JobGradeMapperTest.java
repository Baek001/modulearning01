package kr.or.ddit.mybatis.mapper;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.vo.JobGradeVO;

@Transactional
@SpringBootTest
class JobGradeMapperTest {

	Logger log = LoggerFactory.getLogger(JobGradeMapperTest.class);
	
	@Autowired
	JobGradeMapper mapper;
	
	@Test
	void testSelectJobGeadeList() {
		mapper.selectJobGeadeList().forEach(System.out::println);
	}

	@Test
	void testSelectJobGrade() {
		JobGradeVO jbgdCd = mapper.selectJobGrade("JBGD05");
		assertNotNull(jbgdCd);
	}

}
