package kr.or.ddit.emailContent.service;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import kr.or.ddit.comm.exception.EntityNotFoundException;
import kr.or.ddit.comm.paging.PaginationInfo;
import kr.or.ddit.email.content.service.EmailContentService;
import kr.or.ddit.vo.EmailContentVO;
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
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 26.     	임가영	       최초 생성
 *
 * </pre>
 */
@SpringBootTest
@Slf4j
class EmailContentServiceImplTest {
	
	@Autowired
	EmailContentService service;
	
	EmailContentVO emailContent;
	@BeforeEach
	void setBefore () {
		EmailContentVO evo = new EmailContentVO();
		evo.setUserId("c001");
		evo.setSubject("메일 제목입니당");
		evo.setContent("메일 내용입니당");
		
		service.registerEmailContent(evo);
	}

	@Test
	void testReadEmailContentList() {
		PaginationInfo<EmailContentVO> paging = new PaginationInfo<>();
		List<EmailContentVO> list = service.readEmailContentList(paging);
		
		log.info("======> EmailContentList : {}", list);
		assertNotEquals(0, list.size());
	}


	@Test
	void testReadEmailContent() {
		EmailContentVO evo = service.readEmailContent("MAIL00000003");
		
		log.info("======> EmailBox : {}", evo);
		assertNotNull(evo);
		
		assertThrows(EntityNotFoundException.class, () -> 
			service.readEmailContent("aaad")
		);
	}

}
