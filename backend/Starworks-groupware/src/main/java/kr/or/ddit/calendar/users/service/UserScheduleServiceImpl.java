package kr.or.ddit.calendar.users.service;

import java.util.List;

import org.springframework.stereotype.Service;

import kr.or.ddit.comm.exception.EntityNotFoundException;
import kr.or.ddit.mybatis.mapper.UserScheduleMapper;
import kr.or.ddit.vo.UserScheduleVO;
import lombok.RequiredArgsConstructor;
/**
 * 
 * @author 장어진
 * @since 2025. 9. 26.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 26.     	장어진	          최초 생성
 *
 * </pre>
 */
@Service
@RequiredArgsConstructor
public class UserScheduleServiceImpl implements UserScheduleService{

	private final UserScheduleMapper mapper;
	
	@Override
	public List<UserScheduleVO> readUserScheduleList() {
		return mapper.selectUserScheduleList();
	}

	@Override
	public List<UserScheduleVO> readUserScheduleList(String userId) {
		return mapper.selectUserScheduleListByUserId(userId);
	}

	@Override
	public UserScheduleVO readUserSchedule(String userSchdId) {
		UserScheduleVO vo = mapper.selectUserSchedule(userSchdId);
		if (vo == null) {
			throw new EntityNotFoundException(userSchdId);
		}
		return vo;
	}

	@Override
	public boolean createUserSchedule(UserScheduleVO us) {
		return mapper.insertUserSchedule(us) > 0;
	}

	@Override
	public boolean modifyUserSchedule(UserScheduleVO us) {
		return mapper.updateUserSchedule(us) > 0;
	}

	@Override
	public boolean removeUserSchedule(String userSchdId) {
		return mapper.softDeleteUserSchedule(userSchdId) > 0;
	}

}
