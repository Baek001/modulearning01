package kr.or.ddit.meeting.service;

import java.util.List;

import kr.or.ddit.vo.MeetingRecurringBookingVO;

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
public interface MeetingRecurringBookingService {

	/**
	 * 회의실 반복예약 신청 전체 조회
	 * @param recurringBooking 조회하고 싶은 신청자를 넣은 vo
	 * @return 조회결과 없으면 list.size() == 0
	 */
	public List<MeetingRecurringBookingVO> readRecurringBookingList(MeetingRecurringBookingVO meetingRecurringBooking);

	/**
	 * 회의실 진행중인 반복예약 전체 조회
	 * @param
	 * @return 조회결과 없으면 list.size() == 0
	 */
	public List<MeetingRecurringBookingVO> readProgressRecurringBookingList();

	/**
	 * 회의실 반복예약 신청 단건조회
	 * @param recurringBooking 조회하고싶은 반복예약id 를 담은 vo
	 * @return 조회 결과 없으면 EntityNotFoundException 발생
	 */
	public MeetingRecurringBookingVO readRecurringBooking(MeetingRecurringBookingVO meetingRecurringBooking);

	/**
	 * 회의실 반복예약 등록
	 * @param recurringBooking
	 * @return 성공하면 true, 실패하면 false
	 */
	public boolean createRecurringBooking(MeetingRecurringBookingVO meetingRecurringBooking);

	/**
	 * 회의실 반복예약 정보 수정
	 * @param recurringBooking
	 * @return 성공하면 true, 실패하면 false
	 */
	public boolean modifyRecurringBooking(MeetingRecurringBookingVO meetingRecurringBooking);

	/**
	 * 회의실 반복예약 승인
	 * @param meetingRecurringBooking 반복예약 id 를 담은 vo
	 * @return 성공하면 true, 실패하면 false, 만약 이미 예약된 시간이라면 custom Exceiption
	 */
	public boolean approvalRecurringBooking(MeetingRecurringBookingVO meetingRecurringBooking);

	/**
	 * 회의실 반복에약 반려
	 * @param meetingRecurringBookingVO 반복예약 id, 반려사유를 담은 vo
	 * @return 성공하면 true, 실패하면 false
	 */
	public boolean rejectReucrringBooking(MeetingRecurringBookingVO meetingRecurringBookingVO);

}
