package kr.or.ddit.mybatis.mapper;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.comm.paging.PaginationInfo;
import kr.or.ddit.vo.EmailContentVO;
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
 *  2025. 9. 25.     	홍현택	        최초 생성
 *  2025. 9. 26.		임가영			totalRcoard, emailContentList 테스트 케이스 수정
 *
 * </pre>
 */
@SpringBootTest
@Transactional
@Slf4j
class EmailContentMapperTest {
	
    @Autowired
    EmailContentMapper mapper;

    @Test
    void selectTotalRecord() {
        int total = mapper.selectTotalRecord(new PaginationInfo<EmailContentVO>(1, 5));
        log.info("total count: {}", total);
        assertTrue(total >= 0);
    }

    @Test
    void selectEmailContentList() {
    	PaginationInfo<EmailContentVO> paging = new PaginationInfo<>();
    	int total = mapper.selectTotalRecord(paging);
    	paging.setTotalRecord(total);
    	
        List<EmailContentVO> list =  mapper.selectEmailContentList(paging);
        assertNotEquals(0, list.size());
        log.info("page size: {}", list.size());
    }

    @Test
    void selectEmailContentListNonPaging() {
        List<EmailContentVO> list = assertDoesNotThrow(
            () -> mapper.selectEmailContentListNonPaging()
        );
        assertNotNull(list);
        if (!list.isEmpty()) {
            assertNotNull(list.get(0).getEmailContId());
        }
    }

    @Test
    void selectEmailContent() {
        String userId = "a001";

        EmailContentVO vo = assertDoesNotThrow(
            () -> mapper.selectEmailContent(userId)
        );
        if (vo != null) {
            assertEquals(userId, vo.getEmailContId());
        } else {
            log.warn("EMAIL_CONT_ID={} 데이터가 없습니다.", userId);
        }
    }

//    @Test
//    void insertAndSelect() {
//
//        EmailContentVO Vo = new EmailContentVO();
//        
//        Vo.setEmailContId("MAIL00000003");
//        Vo.setUserId("a001");
//        Vo.setSubject("TEST");
//        Vo.setContent("TEST CONTENT");
//        Vo.setSendDate(LocalDate.now());
//        Vo.setMailFileId(null);
//
//        String inserted = assertDoesNotThrow(
//            () -> mapper.insertEmailContent(newVo)
//           );
//        assertEquals(1, inserted, "정확히 1건이 insert되어야 함");
//
//        EmailContentVO found = mapper.selectEmailContent(newVo.getEmailContId());
//        assertNotNull(found, "insert 직후 단건 조회 결과 null");
//        assertEquals("JUnit Insert Subject", found.getSubject());
//    }
}
