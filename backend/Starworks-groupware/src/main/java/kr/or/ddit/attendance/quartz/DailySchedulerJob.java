package kr.or.ddit.attendance.quartz;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.mybatis.mapper.BusinessTripMapper;
import kr.or.ddit.mybatis.mapper.TimeAndAttendanceMapper;
import kr.or.ddit.mybatis.mapper.UsersMapper;
import kr.or.ddit.mybatis.mapper.VactionMapper;
import kr.or.ddit.vo.BusinessTripVO;
import kr.or.ddit.vo.TimeAndAttendanceVO;
import kr.or.ddit.vo.UsersVO;
import kr.or.ddit.vo.VactionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class DailySchedulerJob extends QuartzJobBean {

	private final TimeAndAttendanceMapper taaMapper;
	private final VactionMapper vactMapper;
	private final BusinessTripMapper bztrMapper;
	private final UsersMapper uMapper;

	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

	@Override
	protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
		log.info("=== 스케줄러 실행 시작 ===");
		log.info("실행 시간: {}", new Date());

		try {
			UpdateAttendanceWhoNotLeave();
			UpdateAttendanceVactBztr();
//			MakeADSTeamMenu(); // 데이터 삽입용
		} catch (Exception e) {
			log.error("스케줄러 실행 중 오류 발생", e);
			throw new JobExecutionException(e);
		}
		log.info("=== 스케줄러 실행 완료 ===");
	}

	@Transactional
	private void UpdateAttendanceWhoNotLeave() {
		List<TimeAndAttendanceVO> taaVOList = taaMapper.selectTimeAndAttendanceList();

		LocalDate today = LocalDate.now();
		taaVOList.forEach(taaVO -> {
			if (taaVO.getWorkEndDt() == null && !taaVO.getWorkBgngDt().toLocalDate().isEqual(today)) {
				LocalDate workDate = taaVO.getWorkBgngDt().toLocalDate();
				LocalTime leaveWork = LocalTime.of(18, 0);
				LocalDateTime setLeaveWork = LocalDateTime.of(workDate, leaveWork);

				if (!taaVO.getWorkBgngDt().isAfter(setLeaveWork)) {
					uMapper.updateWorkStts(taaVO.getUserId(), "C103");

					LocalDateTime endDt = LocalDateTime.of(workDate, LocalTime.of(18, 0, 0));
					LocalDateTime bgngDt = taaVO.getWorkBgngDt();
					LocalDateTime setBeforeLunch = LocalDateTime.of(workDate, LocalTime.of(12, 0));
					LocalDateTime setAfterLunch = LocalDateTime.of(workDate, LocalTime.of(13, 0));

					Duration workHrDuration = Duration.between(bgngDt, endDt);
					Integer workHr = null;

					// 정상 출근
					if (bgngDt.isBefore(setBeforeLunch) && endDt.isAfter(setAfterLunch)) {
						workHr = (int) (workHrDuration.toMinutes() - 60); // 점심시간 보정
						// 점심 전 조퇴 or 점심 후 출근
					} else if (endDt.isBefore(setBeforeLunch) || bgngDt.isAfter(setAfterLunch)) {
						workHr = (int) workHrDuration.toMinutes(); // 점심시간 없으므로 점심시간 계산 제외
						// 점심시간 전 출근 후 점심시간 사이에 퇴근
					} else if (bgngDt.isBefore(setBeforeLunch) && endDt.isAfter(setBeforeLunch)
							&& endDt.isBefore(setAfterLunch)) {
						Duration temp = Duration.between(setBeforeLunch, endDt);
						workHr = (int) (workHrDuration.toMinutes() - temp.toMinutes());
						// 점심시간 중 출근 후 점심시간 이후 퇴근
					} else if (endDt.isAfter(setAfterLunch) && bgngDt.isAfter(setBeforeLunch)
							&& bgngDt.isBefore(setAfterLunch)) {
						Duration temp = Duration.between(bgngDt, setAfterLunch);
						workHr = (int) (workHrDuration.toMinutes() - temp.toMinutes());
						// 점심시간 중 출퇴근
					} else if (bgngDt.isAfter(setBeforeLunch) && endDt.isBefore(setAfterLunch)) {
						workHr = 0;
					}

					if (workHr < 0)
						workHr = -workHr;

					taaVO.setWorkEndDt(endDt);
					taaVO.setWorkHr(workHr);
					taaVO.setEarlyYn("N"); // 18시 퇴근이므로 조퇴 아님
					taaVO.setOvertimeYn("N"); // 18시 퇴근이므로 초과근무 아님
					taaVO.setOvertimeHr(0);

					taaMapper.updateTimeAndAttendance(taaVO);
				}
			}
		});
	}

	@Transactional
	private void UpdateAttendanceVactBztr() {
		List<VactionVO> vactVOList = vactMapper.selectVactionList();
		List<BusinessTripVO> bztrVOList = bztrMapper.selectBusinessTripList();

		LocalDate today = LocalDate.now();
		String todayString = today.format(formatter);

		vactVOList.forEach(vactVO -> {
			LocalDate bgngDate = vactVO.getVactBgngDt().toLocalDate();
			LocalDate endDate = vactVO.getVactEndDt().toLocalDate();
			LocalDateTime endDt = LocalDateTime.of(today, LocalTime.of(18, 0, 0));

			if (!today.isBefore(bgngDate) && !today.isAfter(endDate)) {
				uMapper.updateWorkStts(vactVO.getVactUserId(), "C105");
				TimeAndAttendanceVO oldTaaVO = taaMapper.selectTimeAndAttendance(vactVO.getVactUserId(), todayString);

				TimeAndAttendanceVO taaVO = new TimeAndAttendanceVO();
				taaVO.setWorkYmd(todayString);
				taaVO.setUserId(vactVO.getVactUserId());
				taaVO.setWorkBgngDt(endDt);
				taaVO.setWorkEndDt(endDt);
				taaVO.setEarlyYn("N");
				taaVO.setLateYn("N");
				taaVO.setOvertimeYn("N");
				taaVO.setWorkHr(0);
				taaVO.setOvertimeHr(0);

				if (oldTaaVO == null) {
					taaMapper.insertTimeAndAttendanceForSchedule(taaVO);
					taaMapper.updateTimeAndAttendanceForSchedule(taaVO);
				} else {
					taaMapper.updateTimeAndAttendanceForSchedule(taaVO);
				}
			}
		});

		bztrVOList.forEach(bztrVO -> {
			LocalDate bgngDate = bztrVO.getBztrBgngDt().toLocalDate();
			LocalDate endDate = bztrVO.getBztrEndDt().toLocalDate();
			LocalDateTime endDt = LocalDateTime.of(today, LocalTime.of(18, 0, 0));

			if (!today.isBefore(bgngDate) && !today.isAfter(endDate)) {
				uMapper.updateWorkStts(bztrVO.getBztrUserId(), "C104");
				TimeAndAttendanceVO oldTaaVO = taaMapper.selectTimeAndAttendance(bztrVO.getBztrUserId(), todayString);

				TimeAndAttendanceVO taaVO = new TimeAndAttendanceVO();
				taaVO.setWorkYmd(todayString);
				taaVO.setUserId(bztrVO.getBztrUserId());
				taaVO.setWorkBgngDt(endDt);
				taaVO.setWorkEndDt(endDt);
				taaVO.setEarlyYn("N");
				taaVO.setLateYn("N");
				taaVO.setOvertimeYn("N");
				taaVO.setWorkHr(0);
				taaVO.setOvertimeHr(0);

				if (oldTaaVO == null) {
					taaMapper.insertTimeAndAttendanceForSchedule(taaVO);
					taaMapper.updateTimeAndAttendanceForSchedule(taaVO);
				} else {
					taaMapper.updateTimeAndAttendanceForSchedule(taaVO);
				}
			}
		});
	}

	@Transactional
	private void MakeAllUser() {

	}

	@Transactional
	private void MakeADSTeamMenu() {
		LocalDate today = LocalDate.now();

		List<UsersVO> teamMenuList = uMapper.selectUsersByDept("DP004001");

		teamMenuList.forEach(teamMenu -> {
			String todayYMD = today.format(formatter);

	        if(taaMapper.selectTimeAndAttendance(teamMenu.getUserId(), todayYMD) == null) {
	        	return;
	        }

			LocalTime bgngT = null;
			LocalTime endT = null;
			String early = null;
			String late = null;

			double rnd1 = Math.random();
			double rnd2 = Math.random();
			if (rnd1 <= 0.98) {
				bgngT = LocalTime.of(8, (int) (60 - rnd2 * 30));
				late = "N";
			} else {
				bgngT = LocalTime.of(9, (int) (rnd2 * 30));
				late = "Y";
			}
			LocalDateTime bgngDt = LocalDateTime.of(today, bgngT);

			double rnd3 = Math.random();
			double rnd4 = Math.random();
			double rnd5 = Math.random();
			if (rnd3 <= 0.98) {
				endT = LocalTime.of((int) (18 + rnd4 * 4), (int) (rnd5 * 60));
				early = "N";
			} else {
				endT = LocalTime.of((int) (10 + rnd4 * 8), (int) (rnd5 * 60));
				early = "Y";
			}
			LocalDateTime endDt = LocalDateTime.of(today, endT);

			TimeAndAttendanceVO taaVO = new TimeAndAttendanceVO();
			taaVO.setWorkYmd(todayYMD);
			taaVO.setUserId(teamMenu.getUserId());
			taaVO.setWorkBgngDt(bgngDt);
			taaVO.setWorkEndDt(endDt);
			taaVO.setEarlyYn(early);
			taaVO.setLateYn(late);

			LocalDateTime setLeaveWork = LocalDateTime.of(today, LocalTime.of(18, 0));
			LocalDateTime setBeforeLunch = LocalDateTime.of(today, LocalTime.of(12, 0));
			LocalDateTime setAfterLunch = LocalDateTime.of(today, LocalTime.of(13, 0));

			Duration workHrDuration = Duration.between(bgngDt, endDt);
			Integer workHr = 0;

			// 정상 출근
			if (bgngDt.isBefore(setBeforeLunch) && endDt.isAfter(setAfterLunch)) {
				workHr = (int) (workHrDuration.toMinutes() - 60); // 점심시간 보정
				// 점심 전 조퇴 or 점심 후 출근
			} else if (endDt.isBefore(setBeforeLunch) || bgngDt.isAfter(setAfterLunch)) {
				workHr = (int) workHrDuration.toMinutes(); // 점심시간 없으므로 점심시간 계산 제외
				// 점심시간 전 출근 후 점심시간 사이에 퇴근
			} else if (bgngDt.isBefore(setBeforeLunch) && endDt.isAfter(setBeforeLunch)
					&& endDt.isBefore(setAfterLunch)) {
				Duration temp = Duration.between(setBeforeLunch, endDt);
				workHr = (int) (workHrDuration.toMinutes() - temp.toMinutes());
				// 점심시간 중 출근 후 점심시간 이후 퇴근
			} else if (endDt.isAfter(setAfterLunch) && bgngDt.isAfter(setBeforeLunch)
					&& bgngDt.isBefore(setAfterLunch)) {
				Duration temp = Duration.between(bgngDt, setAfterLunch);
				workHr = (int) (workHrDuration.toMinutes() - temp.toMinutes());
				// 점심시간 중 출퇴근
			} else if (bgngDt.isAfter(setBeforeLunch) && endDt.isBefore(setAfterLunch)) {
				workHr = 0;
			}

	        if (workHr < 0) {
	            return; // 음수일 경우 저장 X
	        }

			taaVO.setWorkHr(workHr);
			taaVO.setOvertimeYn(endDt.isAfter(setLeaveWork) ? "Y" : "N");

            // 초과근무 시간 계산 수정
            Integer overHr = 0;
            if (endDt.isAfter(setLeaveWork)) {
                Duration overHrDuration = bgngDt.isAfter(setLeaveWork)
                    ? Duration.between(bgngDt, endDt)
                    : Duration.between(setLeaveWork, endDt);
                overHr = (int) Math.max(0, overHrDuration.toMinutes());
            }
            taaVO.setOvertimeHr(overHr);
		});
	}
}
