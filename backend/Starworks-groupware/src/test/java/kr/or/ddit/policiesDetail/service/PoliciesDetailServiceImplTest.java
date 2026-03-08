package kr.or.ddit.policiesDetail.service;

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
import kr.or.ddit.policies.service.PoliciesDetailService;
import kr.or.ddit.vo.PoliciesDetailVO;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author 임가영
 * @since 2025. 9. 25.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 25.     	임가영	       최초 생성
 *
 * </pre>
 */
@SpringBootTest
@Transactional
@Slf4j
class PoliciesDetailServiceImplTest {
	
	@Autowired
	PoliciesDetailService service;

	PoliciesDetailVO policiesDetail;
	@BeforeEach
	void setBefore() {
		policiesDetail = new PoliciesDetailVO();
		policiesDetail.setFeatureId("M001");
		policiesDetail.setDeptId("DP001002");
		policiesDetail.setJbgdCd("JBGD03");
		
		service.createPoliciesDetail(policiesDetail);
	}
	
	@Test
	void testReadPoliciesDetailList() {
		assertNotEquals(0, service.readPoliciesDetailList().size());
	}

	@Test
	void testReadPoliciesDetail() {
		assertNotNull(service.readPoliciesDetail("1"));
		policiesDetail.setFeatureId("aaaa");
		assertThrows(EntityNotFoundException.class, () -> 
			service.readPoliciesDetail("1")
		);
	}

	@Test
	void testModifyPoliciesDetail() {
		policiesDetail.setJbgdCd("JBGD05");
		assertTrue(service.modifyPoliciesDetail(policiesDetail));
	}

	@Test
	void testRemovePoliciesDetail() {
//		assertTrue(service.removePoliciesDetail(policiesDetail));
	}

}
