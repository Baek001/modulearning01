package kr.or.ddit.attendance.service.impl;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.attendance.service.TimeAndAttendanceService;
import kr.or.ddit.comm.exception.EntityNotFoundException;
import kr.or.ddit.mybatis.mapper.TimeAndAttendanceMapper;
import kr.or.ddit.mybatis.mapper.UsersMapper;
import kr.or.ddit.vo.TimeAndAttendanceVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author 장어진
 * @since 2025. 9. 26.
 * @see
 *
 *      <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 26.     	장어진	          최초 생성
 *  2025.10. 13.     	장어진	          기능 추가
 *  2025.10. 15.     	장어진	          지각, 조퇴, 추가 근무 여부 리팩토링
 *  2025.10. 16.     	장어진	          minutesPart -> minute으로 수정 (Parts는 나머지 분)
 *
 *      </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TimeAndAttendanceServiceImpl implements TimeAndAttendanceService {

	private final TimeAndAttendanceMapper mapper;
	private final UsersMapper uMapper;

	/**
	 * 근무 기록 전체 조회
	 */
	@Override
	public List<TimeAndAttendanceVO> readTimeAndAttendanceList() {
		return mapper.selectTimeAndAttendanceList();
	}

	/**
	 * 근무기록 조회 (사용자 + 일자)
	 */
	@Override
	public TimeAndAttendanceVO readTimeAndAttendance(String userId, String workYmd) {
		TimeAndAttendanceVO vo = mapper.selectTimeAndAttendance(userId, workYmd);
		if (vo == null) {
			Map<String, String> timeAndAttendance = new HashMap<>();
			timeAndAttendance.put("userId", userId);
			timeAndAttendance.put("workYmd", workYmd);
			throw new EntityNotFoundException(timeAndAttendance);
		}
		return vo;
	}

	/**
	 * 사용자 근무 기록 조회
	 */
	@Override
	public List<TimeAndAttendanceVO> readUserTimeAndAttendanceList(String userId) {
		List<TimeAndAttendanceVO> vo = mapper.selectUserTimeAndAttendance(userId);
		if (vo == null) {
			Map<String, String> timeAndAttendance = new HashMap<>();
			timeAndAttendance.put("userId", userId);
			throw new EntityNotFoundException(timeAndAttendance);
		}
		return vo;
	}

	/**
	 * 근무기록 삽입 (근무 현황 기록도 함께)
	 */
	@Override
	@Transactional
	public boolean createTimeAndAttendance(String userId) {

		int wsCnt = uMapper.updateWorkStts(userId, "C101");

		TimeAndAttendanceVO taaVO = new TimeAndAttendanceVO();

		LocalDate today = LocalDate.now();
		LocalTime startWork = LocalTime.of(9, 0);

		LocalDateTime setStartWork = LocalDateTime.of(today, startWork);
		LocalDateTime bgngDT = LocalDateTime.now();

		taaVO.setUserId(userId);
		taaVO.setWorkBgngDt(bgngDT);
		taaVO.setLateYn(
				bgngDT.isBefore(setStartWork)
						? "N"
						: "Y");

		int taaCnt = mapper.insertTimeAndAttendance(taaVO);

		return wsCnt > 0 && taaCnt > 0;
	}

	/**
	 * 근무 기록 수정 (근무 현황 기록도 함께)
	 */
	@Override
	@Transactional
	public boolean modifyTimeAndAttendance(TimeAndAttendanceVO taaVO) {
		int wsCnt = uMapper.updateWorkStts(taaVO.getUserId(), "C103");

		TimeAndAttendanceVO newTaaVO = mapper.selectTimeAndAttendance(taaVO.getUserId(), taaVO.getWorkYmd());

		LocalDateTime bgngDt = newTaaVO.getWorkBgngDt();
		LocalDateTime endDt = LocalDateTime.now();

		LocalDate workDate = bgngDt.toLocalDate();  // 출근 날짜 기준
		LocalTime leaveWork = LocalTime.of(18, 0);
		LocalTime beforeLunch = LocalTime.of(12, 0);
		LocalTime afterLunch = LocalTime.of(13, 0);

		LocalDateTime setLeaveWork = LocalDateTime.of(workDate, leaveWork);
		LocalDateTime setBeforeLunch = LocalDateTime.of(workDate, beforeLunch);
		LocalDateTime setAfterLunch = LocalDateTime.of(workDate, afterLunch);


		Duration workHrDuration = Duration.between(bgngDt, endDt);
		Integer workHr = null;

		// 정상 출근
		if(bgngDt.isBefore(setBeforeLunch) && endDt.isAfter(setAfterLunch)) {
			workHr = (int) (workHrDuration.toMinutes() - 60); // 점심시간 보정
		// 점심 전 조퇴 or 점심 후 출근
		} else if(endDt.isBefore(setBeforeLunch) || bgngDt.isAfter(setAfterLunch)) {
			workHr = (int) workHrDuration.toMinutes(); // 점심시간 없으므로 점심시간 계산 제외
		// 점심시간 전 출근 후 점심시간 사이에 퇴근
		} else if(bgngDt.isBefore(setBeforeLunch) && endDt.isAfter(setBeforeLunch) && endDt.isBefore(setAfterLunch)) {
			Duration temp = Duration.between(setBeforeLunch, endDt);
			workHr = (int) (workHrDuration.toMinutes() - temp.toMinutes());
		// 점심시간 중 출근 후 점심시간 이후 퇴근
		} else if(endDt.isAfter(setAfterLunch) && bgngDt.isAfter(setBeforeLunch) && bgngDt.isBefore(setAfterLunch)) {
			Duration temp = Duration.between(bgngDt, setAfterLunch);
			workHr = (int) (workHrDuration.toMinutes() - temp.toMinutes());
		// 점심시간 중 출퇴근
		} else if(bgngDt.isAfter(setBeforeLunch) && endDt.isBefore(setAfterLunch)) {
			workHr = 0;
		}

		if(workHr < 0) workHr = -workHr;

		newTaaVO.setWorkHr(workHr);
		newTaaVO.setWorkEndDt(endDt);
		newTaaVO.setEarlyYn(
						endDt.isBefore(setLeaveWork)
						? "Y"
						: "N");

		newTaaVO.setOvertimeYn(
						endDt.isAfter(setLeaveWork)
						? "Y"
						: "N");

		Integer overHr = 0;
		if (endDt.isAfter(setLeaveWork)) {  // 18시 이후일 때만 계산
		    Duration overHrDuration = bgngDt.isAfter(setLeaveWork)
		        ? Duration.between(bgngDt, endDt)
		        : Duration.between(setLeaveWork, endDt);
		    overHr = (int) Math.max(0, overHrDuration.toMinutes());  // 음수 방지
		}
		newTaaVO.setOvertimeHr(overHr);

		int taaCnt = mapper.updateTimeAndAttendance(newTaaVO);

		return wsCnt > 0 && taaCnt > 0;
	}
}
