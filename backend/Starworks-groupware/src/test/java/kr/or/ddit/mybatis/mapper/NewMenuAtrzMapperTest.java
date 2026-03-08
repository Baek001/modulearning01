package kr.or.ddit.mybatis.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.comm.paging.PaginationInfo;
import kr.or.ddit.vo.NewMenuAtrzVO;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author 임가영
 * @since 2025. 9. 25.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 25.     	임가영	       최초 생성
 *
 * </pre>
 */
@Transactional
@SpringBootTest
@Slf4j
class NewMenuAtrzMapperTest {
	
	@Autowired
	NewMenuAtrzMapper mapper;
	
	NewMenuAtrzVO newMenuAtrz;
	
	@BeforeEach
	void setUpBefore() throws Exception {
		newMenuAtrz = new NewMenuAtrzVO();
		newMenuAtrz.setAtrzDocId("ATRZ000000000001");
		newMenuAtrz.setMenuNm("아이스아메리카노");
		newMenuAtrz.setMarketingContent("열심히 홍보하겟다");
		
		mapper.insertNewMenuAtrz(newMenuAtrz);
	}

	@Test
	void testSelectTotalRecord() {
		PaginationInfo<NewMenuAtrzVO> paging = new PaginationInfo<>();
		paging.setCurrentPage(1);
	
		int totalRecord = mapper.selectTotalRecord(paging);
		log.info("======> totalRecord {}", totalRecord);
		
		assertNotEquals(0, totalRecord);
	}

	@Test
	void testSelectNewMenuAtrzList() {
		PaginationInfo<NewMenuAtrzVO> paging = new PaginationInfo<>();
		paging.setCurrentPage(1);
	
		int totalRecord = mapper.selectTotalRecord(paging);
		paging.setTotalRecord(totalRecord);
		List<NewMenuAtrzVO> newMenuAtrzList = mapper.selectNewMenuAtrzList(paging);
		
		System.out.println(newMenuAtrzList);
		assertEquals(1, newMenuAtrzList.size());
	}

	@Test
	void testSelectNewMenuAtrzListNonPaging() {
		List<NewMenuAtrzVO> newMenuAtrzList =  mapper.selectNewMenuAtrzListNonPaging();
		log.info("======> selectNewMenuAtrzListNonPaging {}", newMenuAtrzList);
		
		assertNotEquals(0, newMenuAtrzList.size());
	}

	@Test
	void testSelectNewMenuAtrz() {
		int sqn = newMenuAtrz.getNwmnSqn();
		NewMenuAtrzVO newMenuAtrz = mapper.selectNewMenuAtrz(sqn);
		log.info("======> selectNewMenuAtrz {}", newMenuAtrz);
		
		assertNotNull(newMenuAtrz);
	}

}
