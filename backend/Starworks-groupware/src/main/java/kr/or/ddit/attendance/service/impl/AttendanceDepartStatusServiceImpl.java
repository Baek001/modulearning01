package kr.or.ddit.attendance.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import kr.or.ddit.attendance.service.AttendanceDepartStatusService;
import kr.or.ddit.dto.AttendanceDepartStatusDTO;
import kr.or.ddit.mybatis.mapper.AttendanceDepartStatusMapper;
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
public class AttendanceDepartStatusServiceImpl implements AttendanceDepartStatusService {

	private final AttendanceDepartStatusMapper mapper;

	@Override
	public List<AttendanceDepartStatusDTO> readAttendanceDepartStatusList(AttendanceDepartStatusDTO dto) {
		return mapper.selectAttendanceDepartStatusList(dto);
	}

	@Override
	public List<AttendanceDepartStatusDTO> readAttendanceDepartStatisticsList(AttendanceDepartStatusDTO dto) {
		return mapper.selectAttendanceDepartStatisticsList(dto);
	}

}
