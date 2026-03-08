package kr.or.ddit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import kr.or.ddit.comm.file.service.impl.FileUploadServiceImpl;
import kr.or.ddit.mybatis.mapper.AuthorizationDocumentMapper;
import kr.or.ddit.vo.AuthorizationDocumentVO;
import lombok.extern.slf4j.Slf4j;
/**
 * 
 * @author 임가영
 * @since 2025. 10. 2.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 2.     	임가영           최초 생성
 *
 * </pre>
 */
@SpringBootTest
@Slf4j
class pdfUploadTest {

	@Autowired
	FileUploadServiceImpl service;
	@Autowired
	AuthorizationDocumentMapper mapper;

	
	@Test
	void test() {
		AuthorizationDocumentVO adVO = mapper.selectAuthDocument("ATRZ000000000024", "2020020");
		log.info("=======> adVO: {}", adVO);
		service.savePdfS3(adVO);
	}

}
