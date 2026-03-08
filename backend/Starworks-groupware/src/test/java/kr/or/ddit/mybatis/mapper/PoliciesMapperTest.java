package kr.or.ddit.mybatis.mapper;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.vo.PoliciesVO;
import lombok.extern.slf4j.Slf4j;

@Transactional
@SpringBootTest
@Slf4j
class PoliciesMapperTest {

	@Autowired
	PoliciesMapper mapper;
	
	PoliciesVO policies;
	@BeforeEach
	void setBefore() {
		policies = new PoliciesVO();
		policies.setFeatureId("M001");
		
		mapper.insertPolicies(policies);
	}
	
	@Test
	void testSelectPoliciesList() {
		List<PoliciesVO> policiesList = mapper.selectPoliciesList();
		
		log.info("======> policiesList : {}", policiesList);
		assertNotEquals(0, policiesList.size());
	}

	@Test
	void testSelectPolicies() {
		PoliciesVO policies = mapper.selectPolicies("M001");
		
		log.info("======> policies : {}", policies);
		assertNotNull(policies);
	}

	@Test
	void testUpdatePolicies() {
		policies.setRemark("설명 수정");
		int rowcnt = mapper.updatePolicies(policies);
		
		assertNotEquals(0, rowcnt);
	}

	@Test
	void testDeletePolices() {
		int rowcnt = mapper.deletePolices("M001");
		
		assertNotEquals(0, rowcnt);
	}

}
