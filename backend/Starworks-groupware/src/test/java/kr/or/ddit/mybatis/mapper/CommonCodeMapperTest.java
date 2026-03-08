package kr.or.ddit.mybatis.mapper;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import kr.or.ddit.vo.CommonCodeVO;
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
 *    수정일      		수정자              수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 26.     	임가영	          최초 생성
 *
 * </pre>
 */
@SpringBootTest
@Slf4j
class CommonCodeMapperTest {

	@Autowired
	CommonCodeMapper mapper;
	
	@Test
	void testSelectCommonCodeList() {
		List<CommonCodeVO> list = mapper.selectCommonCodeList("F1");
		
		log.info("======> commonCodeList : {}", list);
		assertNotEquals(0, list.size());
	}

	@Test
	void testSelectCommonCode() {
		CommonCodeVO cvo = mapper.selectCommonCode("A101");
		
		log.info("======> commonCode : {}", cvo);
		assertNotNull(cvo);
	}

}
