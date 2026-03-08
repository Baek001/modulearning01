package kr.or.ddit.commonCodeGroup.service;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import kr.or.ddit.comm.code.service.CommonCodeGroupService;
import kr.or.ddit.comm.exception.EntityNotFoundException;
import kr.or.ddit.vo.CommonCodeGroupVO;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author 임가영
 * @since 2025. 9. 26.
 * @see
 *
 *      <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자               수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 26.     	임가영	          최초 생성
 *
 *      </pre>
 */
@SpringBootTest
@Slf4j
class CommonCodeGroupServiceImplTest {

	@Autowired
	CommonCodeGroupService service;

	@Test
	void testReadCommonCodeGroupList() {
		List<CommonCodeGroupVO> list = service.readCommonCodeGroupList();

		log.info("======> commonCodeGroupList : {}", list);
		assertNotEquals(0, list.size());
	}

	@Test
	void testReadCommonCodeGroup() {
		CommonCodeGroupVO cvo = service.readCommonCodeGroup("A1");

		log.info("======> commonCodeGroup : {}", cvo);
		assertNotNull(cvo);

		assertThrows(EntityNotFoundException.class, () -> service.readCommonCodeGroup("zz"));
	}

}
