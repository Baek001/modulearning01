package kr.or.ddit.mybatis.mapper;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.vo.EmailMappingVO;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author 홍현택
 * @since 2025. 9. 25.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 25.     	홍현택	          최초 생성
 *
 * </pre>
 */
@SpringBootTest
@Transactional
@Slf4j
class EmailMappingMapperTest {

    @Autowired
    EmailMappingMapper mapper;
    
    
    @Test
    void testEmailMappingMapper() {
        EmailMappingVO vo = new EmailMappingVO();
        vo.setMailboxId("MBOX000005");
        vo.setEmailContId("MAIL00000003");
        vo.setReadYn("N");
        vo.setReadDt(null);
        vo.setDelYn("N");
        vo.setDelDt(null);
       
        int insert = assertDoesNotThrow(() -> mapper.insertEmailMapping(vo));
        assertEquals(1, insert);
        
    }
}
