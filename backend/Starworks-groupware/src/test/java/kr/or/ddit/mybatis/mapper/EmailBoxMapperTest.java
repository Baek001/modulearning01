package kr.or.ddit.mybatis.mapper;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.vo.EmailBoxVO;
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
 *  2025. 9. 25.     	홍현택	          Emailbox mapper, xml 테스트
 *
 * </pre>
 */
@SpringBootTest
@Transactional
@Slf4j
class EmailBoxMapperTest {

    @Autowired
    private EmailBoxMapper mapper;

    @Test
    void testselectEmailBoxList() {
        List<EmailBoxVO> emailList = assertDoesNotThrow(
                () -> mapper.selectEmailBoxList()      
         );

        assertNotNull(emailList, "결과 리스트가 null");
        log.info("조회된 메일함 수: {}", emailList.size());

        if (!emailList.isEmpty()) {
            EmailBoxVO first = emailList.get(0);
            assertNotNull(first.getMailboxId());
            log.info("첫 번째 메일함: {}", first);
        }
    }

    @Test
    void testSelectEmailBox() {
        String testMailboxId = "MBOX000005";
        String testMailboxTypeCd = "G101";
        String testMailboxNm = "TEST 메일함";

        EmailBoxVO emailBox = assertDoesNotThrow(
                () -> mapper.selectEmailBox(testMailboxId)
        );
        assertNotNull(emailBox, "단건 조회 결과가 null 입니다.");
        assertEquals(testMailboxId, emailBox.getMailboxId(), "메일함 ID가 일치하지 않습니다.");
        assertEquals(testMailboxTypeCd, emailBox.getMailboxTypeCd(), "메일함 타입 코드가 일치하지 않습니다.");
        assertEquals(testMailboxNm, emailBox.getMailboxNm(), "메일함 이름이 일치하지 않습니다.");
        log.info("조회된 메일함: {}", emailBox);
    }
}
