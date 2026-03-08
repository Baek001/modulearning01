package kr.or.ddit.mybatis.mapper;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import kr.or.ddit.dto.FullCalendarTeamDTO;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author 장어진
 * @since 2025. 9. 30.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 30.     	장어진	          최초 생성
 *
 * </pre>
 */
@Slf4j
@SpringBootTest
class FullCalendarTeamMapperTest {

	@Autowired
	FullCalendarTeamMapper mapper;

	FullCalendarTeamDTO dto;

//	@Test
//	void testSelectFullCalendarTeamList() {
//		dto = new FullCalendarTeamDTO();
//		dto.setUserId("a001");
//		dto.setBizId("BIZ202599999");
//
//		List<FullCalendarTeamDTO> list = mapper.selectFullCalendarTeamList(dto);
//		assertNotEquals(0, list);
//		log.info("============> Result : {}", list);
//	}

}
