package kr.or.ddit.attendance.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import kr.or.ddit.attendance.service.UserMonthlyAttendanceService;
import kr.or.ddit.dto.UserMonthlyAttendanceDTO;
import kr.or.ddit.mybatis.mapper.UserMonthlyAttendanceMapper;
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
public class UserMonthlyAttendanceServiceImpl implements UserMonthlyAttendanceService {

	private final UserMonthlyAttendanceMapper mapper;

	@Override
	public List<UserMonthlyAttendanceDTO> readUserMonthlyAttendanceLateAbsent(UserMonthlyAttendanceDTO dto) {
		return mapper.selectUserMonthlyAttendanceLateAbsent(dto);
	}

	@Override
	public UserMonthlyAttendanceDTO readUserMonthlyAttendance(UserMonthlyAttendanceDTO dto) {
		return mapper.selectUserMonthlyAttendance(dto);
	}


}
