package kr.or.ddit.mybatis.mapper;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.vo.UserHistoryVO;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author 임가영
 * @since 2025. 10. 15.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 15.     	임가영           최초 생성
 *
 * </pre>
 */
@Transactional
@SpringBootTest
@Slf4j
class UserHistoryMapperTest {

	@Autowired
	private UserHistoryMapper mapper;

	UserHistoryVO history = new UserHistoryVO();

	@BeforeEach
	void setBefore() {
		history.setUserId("2026063");
		history.setBeforeJbgdCd("JBGD03");
		history.setAfterJbgdCd("JBGD01");
		history.setBeforeDeptId("DP005001");
		history.setAfterDeptId("DP005002");

		mapper.insertUserHistory(history);
	}


	@Test
	void selectUserHistoryList() {
		// 인사기록 전체 조회
		List<UserHistoryVO> historyList = mapper.selectUserHistoryList();
		log.info("===================================> historyList : {}", historyList);

		assertNotEquals(0, historyList.size());
	}

	@Test
	void selectUserHistoryByDept() {
		// 부서별 인사기록 조회
		List<UserHistoryVO> historyListByDept = mapper.selectUserHistoryByDept(history.getBeforeDeptId()); // 테스트라서 users 데이터가 아직 바뀌지 않음
		log.info("===================================> historyListByDept : {}", historyListByDept);

		assertNotEquals(0, historyListByDept.size());
	}

	@Test
	void selectUserHistoryByUser() {
		// 개인별 인사기록 조회
		List<UserHistoryVO> historyListByUser = mapper.selectUserHistoryByUser(history.getUserId());
		log.info("===================================> historyListByDept : {}", historyListByUser);

		assertNotEquals(0, historyListByUser.size());
	}

	@Test
	void selectUserHistory() {
		// 인사기록 한 건 조회
		UserHistoryVO historyOne = mapper.selectUserHistory(history.getHistoryId());
		log.info("===================================> historyOne : {}", historyOne);

		assertNotNull(historyOne);
	}

//	@Test
	void insertUserHistory() {

	}
}
