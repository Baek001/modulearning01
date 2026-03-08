package kr.or.ddit.policies.service.impl;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@Slf4j
class PoliciesServiceImplTest {

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
	}

	@Test
	void test() {
		String policiesJbgd = "JBGD06";
		log.info("============================> policiesJbgd.substring(5) : {}", policiesJbgd.substring(5));
		log.info("============================> policiesJbgd.substring(4) : {}", policiesJbgd.substring(4));
		log.info("============================> policiesJbgd.substring(3) : {}", policiesJbgd.substring(3));
		log.info("============================> policiesJbgd.substring(2) : {}", policiesJbgd.substring(2));
		log.info("============================> policiesJbgd.substring(1) : {}", policiesJbgd.substring(1));
	}

}
