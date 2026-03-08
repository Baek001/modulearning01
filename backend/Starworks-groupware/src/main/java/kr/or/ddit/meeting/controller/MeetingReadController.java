package kr.or.ddit.meeting.controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import kr.or.ddit.meeting.service.MeetingMemoService;
import kr.or.ddit.meeting.service.MeetingRecurringBookingService;
import kr.or.ddit.meeting.service.MeetingReservationService;
import kr.or.ddit.meeting.service.MeetingRoomService;
import kr.or.ddit.vo.MeetingMemoVO;
import kr.or.ddit.vo.MeetingRecurringBookingVO;
import kr.or.ddit.vo.MeetingReservationVO;
import kr.or.ddit.vo.MeetingRoomVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author 임가영
 * @since 2025. 10. 17.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 17.     	임가영           최초 생성
 *
 * </pre>
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class MeetingReadController {

	private final MeetingRoomService meetingRoomService;
	private final MeetingReservationService meetingReservationService;
	private final MeetingMemoService meetingMemoService;
	private final MeetingRecurringBookingService recurringBookingService;

	/**
	 * 회의 메인페이지로 이동
	 * @return
	 */
	@GetMapping("/meeting/main")
	public String meetingMain(
		Model model
		, Authentication authentication
		, @RequestParam(required = false) String day) {

		String username = authentication.getName();

		// 회의실 목록
		List<MeetingRoomVO> roomList = meetingRoomService.readMeetingRoomList();
		// 회의 메모 목록
		MeetingMemoVO memoVO = new MeetingMemoVO();
		memoVO.setUserId(username);
		List<MeetingMemoVO> memoList = meetingMemoService.readMeetingMemoList(memoVO);

		// 회의실 예약 목록
		List<MeetingReservationVO> reservationList = new ArrayList<>();
		// 날짜 표시
		String formmattedDate = "";

		int currentHour = LocalTime.now().getHour(); // 현재 시각
		LocalDate today = LocalDate.now();
		if (day != null && !day.isBlank()) {
			LocalDate selectedDay = LocalDate.parse(day); // 선택된 날짜로 LocalDate 구함
			MeetingReservationVO mrVO = new MeetingReservationVO();
			mrVO.setMeetingDate(selectedDay);
			reservationList = meetingReservationService.readMeetingReservationList(mrVO);

			formmattedDate = selectedDay.format(DateTimeFormatter.ofPattern("yyyy-MM-dd (E)"));
			model.addAttribute("selectedDay", selectedDay);
		} else {
			// 오늘 회의 예약 목록
			MeetingReservationVO mrVO = new MeetingReservationVO();
			mrVO.setMeetingDate(today);
			reservationList = meetingReservationService.readMeetingReservationList(mrVO);

			LocalDate localDate = LocalDate.now();
			formmattedDate = localDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd (E)"));
		}

		// 나의 반복예약 신청 리스트
        MeetingRecurringBookingVO recurringBookingVO = new MeetingRecurringBookingVO();
        recurringBookingVO.setUserId(username);
        List<MeetingRecurringBookingVO> myRecurringBookingList = recurringBookingService.readRecurringBookingList(recurringBookingVO);

        // 진행중인 반복예약 리스트
        List<MeetingRecurringBookingVO> progressRecurringBookingList = recurringBookingService.readProgressRecurringBookingList();

		model.addAttribute("currentHour", currentHour); // 현재 시각
		model.addAttribute("today", today); // 오늘 날짜(LocalDate)
		model.addAttribute("day", formmattedDate); // 선택 날짜 (String)
        model.addAttribute("roomList", roomList); // 회의실 리스트
        model.addAttribute("reservationList", reservationList); // 예약 리스트
        model.addAttribute("memoList", memoList); // 메모 리스트

        model.addAttribute("myRecurringBookingList", myRecurringBookingList); // 나의 반복예약 신청 리스트
        model.addAttribute("progressRecurringBookingList", progressRecurringBookingList); // 진행중인 반복예약 리스트

		return "meeting/meeting-main";
	}


}
