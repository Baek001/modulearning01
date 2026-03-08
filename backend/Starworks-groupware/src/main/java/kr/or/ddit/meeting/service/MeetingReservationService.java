package kr.or.ddit.meeting.service;

import java.util.List;

import kr.or.ddit.vo.MeetingReservationVO;
import kr.or.ddit.vo.UsersVO;

/**
 *
 * @author 윤서현
 * @since 2025. 10. 18.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 18.     	윤서현	          최초 생성
 *
 * </pre>
 */
public interface MeetingReservationService {

	/**
	 * 회의실 예약 등록
	 * @param meetingReservation
	 * @return
	 */
	public MeetingReservationVO createMeetingReservation(MeetingReservationVO meetingReservation);

	/**
	 * 회의실 예약 (오버로드 - AI/WebSocket용, UsersVO 직접 전달)
	 */
	public MeetingReservationVO createMeetingReservation(MeetingReservationVO meetingReservation, UsersVO user);
	
	/**
	 * 회의실 예약 리스트 조회
	 * @return
	 */
	public List<MeetingReservationVO> readMeetingReservationList(MeetingReservationVO reservation);

	/**
	 * 회의실 예약 상세조회
	 * @param reservationId
	 * @return
	 */
	public MeetingReservationVO readMeetingReservation(int reservationId);

	/**
	 * 회의실 예약 수정
	 * @param meetingReservation
	 * @return
	 */
	public MeetingReservationVO modifyMeetingReservation(MeetingReservationVO meetingReservation);

	/**
	 * 회의실 예약 취소
	 * @param reservationId
	 * @return
	 */
	public boolean removeMeetingReservation(Integer reservationId);

	/**
	 * 반복예약한 회의실 예약 취소
	 * @param reservationId
	 * @return
	 */
	public boolean removeRecurringBookingMeetingReservation(Integer recurringId);

}
