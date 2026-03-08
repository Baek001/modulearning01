package kr.or.ddit.policies.service;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.comm.exception.EntityNotFoundException;
import kr.or.ddit.vo.PoliciesVO;
import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@Slf4j
@Transactional
class PoliciesServiceImplTest {
	
	@Autowired
	PoliciesService service;
	
	PoliciesVO policy;
	@BeforeEach
	void setBefore() {
//		policy = new PoliciesVO();
//		policy.setFeatureId("M001");
//		policy.setRemark("설명입니당");
//		
//		service.createPolicies(policy);
	}

	@Test
	void testReadPoliciesList() {
		assertNotEquals(0, service.readPoliciesList().size());
	}

	@Test
	void testReadPolicies() {
		assertNotNull(service.readPolicies("M001"));
		assertThrows(EntityNotFoundException.class, () -> 
			service.readPolicies("FFFF")
		);
	}

	@Test
	void testUpdatePolicies() {
		policy.setRemark("수정입니당");
//		assertTrue(service.modifyPolicies(policy));
	}

	@Test
	void testDeletePolicies() {
		assertTrue(service.removePolicies("M001"));
	}

}
