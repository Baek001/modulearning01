package kr.or.ddit.mybatis.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import kr.or.ddit.vo.MeetingRecurringBookingVO;

/**
 *
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
@Mapper
public interface MeetingRecurringBookingMapper {

	/**
	 * 회의실 반복예약 신청 전체 조회
	 * @param recurringBooking 조회하고 싶은 신청자를 넣은 vo
	 * @return 조회결과 없으면 list.size() == 0
	 */
	public List<MeetingRecurringBookingVO> selectRecurringBookingList(MeetingRecurringBookingVO recurringBooking);

	/**
	 * 회의실 진행중인 반복예약 전체 조회
	 * @param
	 * @return 조회결과 없으면 list.size() == 0
	 */
	public List<MeetingRecurringBookingVO> selectProgressRecurringBookingList();

	/**
	 * 회의실 반복예약 신청 단건조회
	 * @param recurringBooking 조회하고싶은 반복예약id 를 담은 vo
	 * @return 조회 결과 없으면 null
	 */
	public MeetingRecurringBookingVO selectRecurringBooking(MeetingRecurringBookingVO recurringBooking);

	/**
	 * 회의실 반복예약 등록
	 * @param recurringBooking
	 * @return 성공한 레코드 수
	 */
	public int insertRecurringBooking(MeetingRecurringBookingVO recurringBooking);

	/**
	 * 회의실 반복예약 정보 수정
	 * @param recurringBooking
	 * @return 성공한 레코드 수
	 */
	public int updateRecurringBooking(MeetingRecurringBookingVO recurringBooking);

	/**
	 * 회의실 반복예약 삭제
	 * @param recurringBooking 삭제할 반복예약 id를 담은 vo
	 * @return 성공한 레코드 수
	 */
	public int deleteRecurringBooking(MeetingRecurringBookingVO recurringBooking);

	//////////////// 아래는 반복예약 반복 요일 관련 ////////////////

	/**
	 * 회의실 반복예약 반복요일 조회
	 * @param recurringId 반복예약로그 id
	 * @return 조회 결과 없으면 list.size() == 0
	 */
	public List<String> selectRecurringWeekdayList(String recurringId);

	/**
	 * 회의실 반복예약 반복요일 등록
	 * @param recurringBooking 반복예약로그 id 와 반복 요일을 담은 vo
	 * @return 성공한 레코드 수
	 */
	public int insertRecurringWeekday(MeetingRecurringBookingVO recurringBooking);

	/**
	 * 회의실 반복예약 반복요일 삭제
	 * @param recurringBooking 삭제할 반복예약로그 id
	 * @return 성공한 레코드 수
	 */
	public int deleteRecurringWeekday(MeetingRecurringBookingVO recurringBooking);

}
