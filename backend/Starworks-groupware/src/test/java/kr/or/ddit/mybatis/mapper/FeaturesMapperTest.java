package kr.or.ddit.mybatis.mapper;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.vo.FeaturesVO;
import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@Transactional
@Slf4j
class FeaturesMapperTest {

	@Autowired
	FeaturesMapper mapper;
	
	@Test
	void testSelectFeaturesList() {
		List<FeaturesVO> featuresList = mapper.selectFeaturesList();
		
		log.info("======> featuresList : {}", featuresList);
		assertNotEquals(0, featuresList.size());
	}

	@Test
	void testSelectFeatures() {
		FeaturesVO feature = mapper.selectFeatures("M001");
		
		log.info("======> feautre : {}", feature);
		assertNotNull(feature);
	}

}
