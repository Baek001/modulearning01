package kr.or.ddit.mybatis.mapper;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.vo.PoliciesDetailVO;
import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@Slf4j
@Transactional
class PoliciesDetailMapperTest {
	
	@Autowired
	PoliciesDetailMapper mapper;
	
	PoliciesDetailVO policiesDetail;
	@BeforeEach
	void setBefore() {
		policiesDetail = new PoliciesDetailVO();
		policiesDetail.setFeatureId("M001");
		policiesDetail.setDeptId("DP001002");
		policiesDetail.setJbgdCd("JBGD03");
		
		mapper.insertPoliciesDetail(policiesDetail);
	}

	@Test
	void testSelectPoliciesDetailList() {
		List<PoliciesDetailVO> policiesDetailList = mapper.selectPoliciesDetailList();
		
		log.info("======> policiesDetailList : {}", policiesDetailList);
		assertNotEquals(0, policiesDetailList.size());
	}

	@Test
	void testSelectPoliciesDetail() {
		assertNotNull(mapper.selectPoliciesDetail("1"));
	}

	@Test
	void testUpdatePoliciesDetailJdgdCd() {
		policiesDetail.setJbgdCd("JBGD05");
		assertNotEquals(0, mapper.updatePoliciesDetailJdgdCd(policiesDetail));
	}

	@Test
	void testDeletePoliciesDetail() {
//		assertNotEquals(0, mapper.deletePoliciesDetail(policiesDetail));
	}

}
