package kr.or.ddit.users.service.impl;

import java.util.List;

import org.springframework.aop.ThrowsAdvice;
import org.springframework.stereotype.Service;

import kr.or.ddit.comm.exception.EntityNotFoundException;
import kr.or.ddit.mybatis.mapper.UserHistoryMapper;
import kr.or.ddit.users.service.UserHistoryService;
import kr.or.ddit.vo.UserHistoryVO;
import lombok.RequiredArgsConstructor;

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
@Service
@RequiredArgsConstructor
public class UserHistoryServiceImpl implements UserHistoryService{

	private final UserHistoryMapper mapper;

	/**
	 * 인사기록 전체 조회
	 */
	@Override
	public List<UserHistoryVO> readUserHistoryList() {
		return mapper.selectUserHistoryList();
	}

	/**
	 * 부서별 인사기록 조회
	 */
	@Override
	public List<UserHistoryVO> readUserHistoryByDept(String deptId) {
		return mapper.selectUserHistoryByDept(deptId);
	}

	/**
	 * 개인별 인사기록 조회
	 */
	@Override
	public List<UserHistoryVO> readUserHistoryByUser(String userId) {
		return mapper.selectUserHistoryByUser(userId);
	}

	/**
	 * 인사기록 한 건 조회
	 */
	@Override
	public UserHistoryVO readUserHistory(Integer historyId) {
		UserHistoryVO history = mapper.selectUserHistory(historyId);

		if(history == null) {
			throw new EntityNotFoundException(history);
		}

		return history;
	}

	/**
	 * 인사기록 데이터 삽입
	 */
	@Override
	public boolean createUserHistory(UserHistoryVO userHistory) {
		return mapper.insertUserHistory(userHistory) > 0;
	}

}
