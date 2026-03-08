package kr.or.ddit.features.service;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.comm.exception.EntityNotFoundException;
import kr.or.ddit.vo.FeaturesVO;
import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@Transactional
@Slf4j
class FeaturesServiceImplTest {
	
	@Autowired
	FeaturesService service;

	@Test
	void testReadFeaturesList() {
		List<FeaturesVO> featuresList = service.readFeaturesList();
		
		log.info("======> featuresList : {}", featuresList);
		assertNotEquals(0, featuresList.size());
	}

	@Test
	void testReadFeatures() {
		FeaturesVO features1 = service.readFeatures("M005-04");
		
		log.info("======> features : {}", features1);
		assertNotNull(features1);
		
		
		assertThrows(EntityNotFoundException.class, () -> 
			service.readFeatures("asdf")
		);
	}


}
