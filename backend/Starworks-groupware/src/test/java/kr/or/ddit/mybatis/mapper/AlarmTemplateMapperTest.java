package kr.or.ddit.mybatis.mapper;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.vo.AlarmTemplateVO;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author 임가영
 * @since 2025. 9. 26.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           	  수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 26.     	임가영	          최초 생성
 *
 * </pre>
 */
@SpringBootTest
@Transactional
@Slf4j
class AlarmTemplateMapperTest {

	@Autowired
	AlarmTemplateMapper mapper;
	
	@Test
	void testSelectAlarmTemplateList() {
		List<AlarmTemplateVO> AlarmTemplateList = mapper.selectAlarmTemplateList();
		
		log.info("======> AlarmTemplateList : {}", AlarmTemplateList);
		assertNotEquals(0, AlarmTemplateList.size());
	}

	@Test
	void testSelectAlarmTemplate() {
		AlarmTemplateVO AlarmTemplate = mapper.selectAlarmTemplate("ALRM9999");
		
		log.info("======> AlarmTemplate : {}", AlarmTemplate);
		assertNotNull(AlarmTemplate);
	}

}
