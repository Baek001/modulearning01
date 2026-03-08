package kr.or.ddit.meeting.service.impl;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.meeting.exception.AlreadyReservationException;
import kr.or.ddit.meeting.service.MeetingRecurringBookingService;
import kr.or.ddit.mybatis.mapper.MeetingRecurringBookingMapper;
import kr.or.ddit.mybatis.mapper.MeetingReservationMapper;
import kr.or.ddit.security.CustomUserDetails;
import kr.or.ddit.vo.MeetingRecurringBookingVO;
import kr.or.ddit.vo.MeetingReservationVO;
import kr.or.ddit.vo.UsersVO;
import lombok.RequiredArgsConstructor;

/**
 * 회의실 반복예약 관련 service
 * @author 임가영
 * @since 2025. 10. 27.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 27.     	임가영           최초 생성
 *
 * </pre>
 */
@Service
@RequiredArgsConstructor
public class MeetingRecurringBookingServiceImpl implements MeetingRecurringBookingService {

	private final MeetingRecurringBookingMapper mapper;
	private final MeetingReservationMapper reservationMapper;

	/**
	 * 회의실 반복예약 신청 전체 조회
	 * @param recurringBooking 조회하고 싶은 신청자를 넣은 vo
	 * @return 조회결과 없으면 list.size() == 0
	 */
	@Override
	public List<MeetingRecurringBookingVO> readRecurringBookingList(MeetingRecurringBookingVO meetingRecurringBooking) {
		return mapper.selectRecurringBookingList(meetingRecurringBooking);
	}

	/**
	 * 회의실 진행중인 반복예약 전체 조회
	 * @param
	 * @return 조회결과 없으면 list.size() == 0
	 */
	@Override
	public List<MeetingRecurringBookingVO> readProgressRecurringBookingList() {
		return mapper.selectProgressRecurringBookingList();
	}

	/**
	 * 회의실 반복예약 신청 단건조회
	 * @param recurringBooking 조회하고싶은 반복예약id 를 담은 vo
	 * @return 조회 결과 없으면 EntityNotFoundException 발생
	 */
	@Override
	public MeetingRecurringBookingVO readRecurringBooking(MeetingRecurringBookingVO meetingRecurringBooking) {
		return mapper.selectRecurringBooking(meetingRecurringBooking);
	}

	/**
	 * 회의실 반복예약 등록
	 * @param recurringBooking
	 * @return 성공한 레코드 수
	 */
	@Override
	@Transactional
	public boolean createRecurringBooking(MeetingRecurringBookingVO meetingRecurringBooking) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
		UsersVO realUser = userDetails.getRealUser();

		// 회의 제목 입력하지 않으면 사용자 + 부서명 + "반복예약"
		String reservationTitle = meetingRecurringBooking.getTitle() != null ? meetingRecurringBooking.getTitle() : "";
		if(reservationTitle.isBlank()) {
			reservationTitle = realUser.getUserNm() + " (" + realUser.getDeptNm() + ") 반복예약";
			meetingRecurringBooking.setTitle(reservationTitle);
		}

		int rowcnt = mapper.insertRecurringBooking(meetingRecurringBooking);

		Integer recurringId = meetingRecurringBooking.getRecurringId();

		// 요일을 선택했다면
		List<String> weekCheckList= meetingRecurringBooking.getWeekCheckList();
		if(weekCheckList != null && !weekCheckList.isEmpty()) {

			for(String weekCheck : weekCheckList) {
				MeetingRecurringBookingVO insertWeekdayVO = new MeetingRecurringBookingVO();
				insertWeekdayVO.setRecurringId(recurringId); // 반복예약로그id
				insertWeekdayVO.setWeekday(weekCheck); // 선택한 요일

				rowcnt += mapper.insertRecurringWeekday(insertWeekdayVO);
			}
		}

		return rowcnt > 0;
	}

	/**
	 * 회의실 반복예약 정보 수정
	 * @param recurringBooking
	 * @return 성공한 레코드 수
	 */
	@Override
	public boolean modifyRecurringBooking(MeetingRecurringBookingVO meetingRecurringBooking) {
		return mapper.updateRecurringBooking(meetingRecurringBooking) > 0;
	}

	/**
	 * 회의실 반복예약 승인
	 */
	@Override
	@Transactional
	public boolean approvalRecurringBooking(MeetingRecurringBookingVO meetingRecurringBooking) {
		int rowcnt = 0;

		// id 가지고 반복예약신청 정보 가져오기
		meetingRecurringBooking = mapper.selectRecurringBooking(meetingRecurringBooking);

		// 신청한 시간이 기존 날짜와 겹치지 않아야 함

		// 만약 겹치지 않는다면 meetingReservation 테이블에 insert
		// 1. startDate ~ endDate 반복문
		// 2-1. 만약 meetingRecurringBooking.getFrequency 가 'day' 라면
		//		startDate 부터 endDate 까지 n일마다 회의실 예약 (주말 및 공휴일 제외)

		String frequency = meetingRecurringBooking.getFrequency(); // 반복 빈도 (일, 주)
		Integer interval = meetingRecurringBooking.getInterval(); // 반복 주기

		LocalDate startDate = meetingRecurringBooking.getStartDate(); // 반복 시작일
		LocalDate endDate = meetingRecurringBooking.getEndDate(); // 반복 종료일
		LocalDate plusDate = startDate;

		List<String> weekCheckList = meetingRecurringBooking.getWeekCheckList(); // 선택된 요일

		MeetingReservationVO newReservationVO = new MeetingReservationVO(); // 아직 insert 안한건 예약 날짜 뿐..
		newReservationVO.setRoomId(meetingRecurringBooking.getRoomId());
		newReservationVO.setUserId(meetingRecurringBooking.getUserId());
		newReservationVO.setStartTime(meetingRecurringBooking.getStartTime());
		newReservationVO.setEndTime(meetingRecurringBooking.getEndTime());
		newReservationVO.setTitle(meetingRecurringBooking.getTitle());
		newReservationVO.setRecurringId(meetingRecurringBooking.getRecurringId()); // 예약 id 세팅

		//////////////////////////  해당 시간대에 회의가 예약되어있는지 확인  //////////////////////////
		if(frequency.equals("day")) {
			while (!plusDate.isAfter(endDate)) { // 반복 시작일이 끝나는 날짜보다 같거나 이전인 경우만 반복

				// plus 된 날짜가 주말이라면 날짜 더하고 다시..
				DayOfWeek day = plusDate.getDayOfWeek();
		        if (day == DayOfWeek.SATURDAY) {
		        	plusDate = plusDate.plusDays(2); // 토요일 -> 월요일
		            continue;
		        } else if (day == DayOfWeek.SUNDAY) {
		        	plusDate = plusDate.plusDays(1); // 일요일 -> 월요일
		            continue;
		        }

				newReservationVO.setMeetingDate(plusDate);
				// 해당 시간대에 회의가 예약되어있는지 리스트 가져옴
				List<MeetingReservationVO> existReservationList = reservationMapper.selectExistMeetingReservation(newReservationVO);
				if (!existReservationList.isEmpty()) {
					throw new AlreadyReservationException(plusDate +" 예약과 중복됩니다.");
				}
				plusDate = plusDate.plusDays(interval); // 반복주기만큼 플러스
			}
		} else if (frequency.equals("week")) {
			// 2-2. 만약 meetingRecurringBooking.getFrequency 가 'week' 라면
			//		startDate 부터 endDate 까지 n주마다 n, n 요일마다 회의실 예약

			while (!plusDate.isAfter(endDate)) { // 반복 시작일이 끝나는 날짜보다 같거나 이전인 경우만 반복

				DayOfWeek inListCheckValue;

				// 하루 증가
				for(String weekCheck : weekCheckList) {
					switch (weekCheck) {
						case "1": inListCheckValue = DayOfWeek.MONDAY; break;
						case "2": inListCheckValue = DayOfWeek.TUESDAY; break;
						case "3": inListCheckValue = DayOfWeek.WEDNESDAY; break;
						case "4": inListCheckValue = DayOfWeek.THURSDAY; break;
						case "5": inListCheckValue = DayOfWeek.FRIDAY; break;
						default: continue;
					}

					int diff = inListCheckValue.getValue() - plusDate.getDayOfWeek().getValue(); // 체크된 요일 - 현재 요일
					if (diff < 0) diff += 7; // 체크된 요일 - 현재 요일이 마이너스라면 이미 지난요일 ex) 수(3) - 금(5) = 마이너스값(-2) => 그래서 7을 더해서(5) 같은 요일로 이동
					LocalDate targetDate = plusDate.plusDays(diff);

					// 대상이되는 날짜가 아직 끝나는 날짜 이전이라면 .. 예약테이블에 insert
					if (!targetDate.isAfter(endDate)) {
						newReservationVO.setMeetingDate(targetDate);
						// 해당 시간대에 회의가 예약되어있는지 리스트 가져옴
						List<MeetingReservationVO> existReservationList = reservationMapper.selectExistMeetingReservation(newReservationVO);
						if (!existReservationList.isEmpty()) {
							throw new AlreadyReservationException(plusDate + " 예약과 중복됩니다.");
						}
					}
				}

				// 요일비교 끝나면 주기만큼 주를 이동
				plusDate = plusDate.plusWeeks(interval);

			} // plusDays 반복문 끝
		}

		////////////////////////////////////////////////////////////////////////////////////

		plusDate = startDate;
		if(frequency.equals("day")) {
			while (!plusDate.isAfter(endDate)) { // 반복 시작일이 끝나는 날짜보다 같거나 이전인 경우만 반복

				// plus 된 날짜가 주말이라면 날짜 더하고 다시..
				DayOfWeek day = plusDate.getDayOfWeek();
		        if (day == DayOfWeek.SATURDAY) {
		        	plusDate = plusDate.plusDays(2); // 토요일 -> 월요일
		            continue;
		        } else if (day == DayOfWeek.SUNDAY) {
		        	plusDate = plusDate.plusDays(1); // 일요일 -> 월요일
		            continue;
		        }

				newReservationVO.setMeetingDate(plusDate);
				reservationMapper.insertMeetingReservation(newReservationVO); // 회의실 예약 테이블에 insert
				plusDate = plusDate.plusDays(interval); // 반복주기만큼 플러스
			}
		} else if (frequency.equals("week")) {
			// 2-2. 만약 meetingRecurringBooking.getFrequency 가 'week' 라면
			//		startDate 부터 endDate 까지 n주마다 n, n 요일마다 회의실 예약

			while (!plusDate.isAfter(endDate)) { // 반복 시작일이 끝나는 날짜보다 같거나 이전인 경우만 반복

				DayOfWeek inListCheckValue;

				// 하루 증가
				for(String weekCheck : weekCheckList) {
					switch (weekCheck) {
						case "1": inListCheckValue = DayOfWeek.MONDAY; break;
						case "2": inListCheckValue = DayOfWeek.TUESDAY; break;
						case "3": inListCheckValue = DayOfWeek.WEDNESDAY; break;
						case "4": inListCheckValue = DayOfWeek.THURSDAY; break;
						case "5": inListCheckValue = DayOfWeek.FRIDAY; break;
						default: continue;
					}

					int diff = inListCheckValue.getValue() - plusDate.getDayOfWeek().getValue(); // 체크된 요일 - 현재 요일
					if (diff < 0) diff += 7; // 체크된 요일 - 현재 요일이 마이너스라면 이미 지난요일 ex) 수(3) - 금(5) = 마이너스값(-2) => 그래서 7을 더해서(5) 같은 요일로 이동
					LocalDate targetDate = plusDate.plusDays(diff);

					// 대상이되는 날짜가 아직 끝나는 날짜 이전이라면 .. 예약테이블에 insert
					if (!targetDate.isAfter(endDate)) {
						newReservationVO.setMeetingDate(targetDate);
				        reservationMapper.insertMeetingReservation(newReservationVO);
					}
				}

				// 요일비교 끝나면 주기만큼 주를 이동
				plusDate = plusDate.plusWeeks(interval);

			} // plusDays 반복문 끝
		}

		// meetingRecurringBooking 테이블에 status 컬럼 'A401' update
		meetingRecurringBooking.setStatus("A401"); // 승인 공통코드
		// 반복예약신청 테이블 정보 업데이트
		rowcnt += mapper.updateRecurringBooking(meetingRecurringBooking);

		return rowcnt > 0;
	}

	/**
	 * 회의실 반복에약 반려
	 */
	@Override
	public boolean rejectReucrringBooking(MeetingRecurringBookingVO meetingRecurringBookingVO) {

		// meetingRecurringBooking 테이블에 status 컬럼 'A402', rejectReason 컬럼 '내용' update
		meetingRecurringBookingVO.setStatus("A402"); // 반려 공통코드
		int rowcnt = mapper.updateRecurringBooking(meetingRecurringBookingVO);

		return rowcnt > 0;
	}

}
