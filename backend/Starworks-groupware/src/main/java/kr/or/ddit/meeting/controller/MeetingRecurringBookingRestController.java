package kr.or.ddit.meeting.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import kr.or.ddit.meeting.exception.AlreadyReservationException;
import kr.or.ddit.meeting.service.MeetingRecurringBookingService;
import kr.or.ddit.meeting.service.MeetingReservationService;
import kr.or.ddit.vo.MeetingRecurringBookingVO;
import lombok.RequiredArgsConstructor;

/**
 * 회의실 반복예약 관련 restController
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
@RestController
@RequiredArgsConstructor
public class MeetingRecurringBookingRestController {

	private final MeetingRecurringBookingService serivce;
	private final MeetingReservationService reservationService;

	/**
	 * 회의실 반복예약 신청 조회
	 * @return
	 */
	@GetMapping("/rest/meeting-recurring-booking")
	public List<MeetingRecurringBookingVO> readMeetingRecurringBookingList(
		@RequestParam(required = false) String status
	) {

		MeetingRecurringBookingVO recurringBooking = new MeetingRecurringBookingVO();
		if (status != null) {
			if (status.equals("progress")) {
				return serivce.readProgressRecurringBookingList();
			}
			recurringBooking.setStatus(status);
		}
		return serivce.readRecurringBookingList(recurringBooking);
	}

	/**
	 * 회의실 반복예약 신청
	 * @param recurringBooking
	 * @return
	 */
	@PostMapping("/rest/meeting-recurring-booking")
	public Map<String, Object> createMeetingRecurringBooking(
		@RequestBody MeetingRecurringBookingVO recurringBooking
	) {
		Map<String, Object> respMap = new HashMap<>();
		boolean success = false;

		success = serivce.createRecurringBooking(recurringBooking);

		respMap.put("success", success);
		return respMap;
	}

	/**
	 * 회의실 반복예약 신청 승인
	 * @return 성공여부를 담은 Map
	 */
	@PostMapping("/rest/meeting-recurring-booking/{recurringId}")
	public Map<String, Object> approvalMeetingRecurringBooking(
		@RequestParam(required = true, name = "type") String type
		, @PathVariable(required = true, name = "recurringId") Integer recurringId
		, @RequestBody(required = false) MeetingRecurringBookingVO meetingRecurringBooking
	){
		Map<String, Object> respMap = new HashMap<>();
		boolean success = false;

		  if (meetingRecurringBooking == null) {
	        meetingRecurringBooking = new MeetingRecurringBookingVO();
	    }

		meetingRecurringBooking.setRecurringId(recurringId);

		if (type.equals("approval")) {
			try {
				success = serivce.approvalRecurringBooking(meetingRecurringBooking);
			} catch (AlreadyReservationException e) {
				respMap.put("message", e.getMessage());
			}
		} else if (type.equals("reject")){
			success = serivce.rejectReucrringBooking(meetingRecurringBooking);
		}

		respMap.put("success", success);
		return respMap;
	}

	@DeleteMapping("/rest/meeting-recurring-booking/{recurringId}")
	public Map<String, Object> removeRecurringBookingReservationBooking(
		@PathVariable(required = true, name = "recurringId") Integer recurringId
	){
		Map<String, Object> respMap = new HashMap<>();
		boolean success = false;

		success = reservationService.removeRecurringBookingMeetingReservation(recurringId);

		respMap.put("success", success);
		return respMap;
	}


}
