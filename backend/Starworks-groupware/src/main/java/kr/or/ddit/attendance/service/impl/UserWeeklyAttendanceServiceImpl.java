package kr.or.ddit.attendance.service.impl;

import org.springframework.stereotype.Service;

import kr.or.ddit.attendance.service.UserWeeklyAttendanceService;
import kr.or.ddit.dto.UserWeeklyAttendanceDTO;
import kr.or.ddit.mybatis.mapper.UserWeeklyAttendanceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author 장어진
 * @since 2025. 10. 15.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 15.     	장어진	          최초 생성
 *
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserWeeklyAttendanceServiceImpl implements UserWeeklyAttendanceService {

	private final UserWeeklyAttendanceMapper mapper;

	@Override
	public UserWeeklyAttendanceDTO readUserWeeklyAttendance(UserWeeklyAttendanceDTO dto) {
		return mapper.selectUserWeeklyAttendance(dto);
	}
}
