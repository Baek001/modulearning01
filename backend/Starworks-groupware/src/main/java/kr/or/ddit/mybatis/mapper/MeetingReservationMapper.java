package kr.or.ddit.mybatis.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import kr.or.ddit.vo.MeetingReservationVO;

/**
 *
 * @author 윤서현
 * @since 2025. 10. 17.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자             수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 17.     	윤서현	         최초 생성
 *  2025. 10. 22.		임가영			 이미 있는 회의실인지 조회하는 mapper 추가
 * </pre>
 */
@Mapper
public interface MeetingReservationMapper {

	/**
	 * 예약된 회의실 상세조회
	 * @param reservationId
	 * @return
	 */
	public MeetingReservationVO selectMeetingReservation(int reservationId);

	/**
	 * 예약된 회의실 리스트 조회
	 * @param reservation 조회하고 싶은 날짜, 조회하고 싶은 roomId 를 담은 VO (전체 조회는 아무것도 넣지 않음)
	 * @return 조회 결과 없으면 list.size() == 0
	 */
	public List<MeetingReservationVO> selectMeetingReservationList(MeetingReservationVO reservation);

	/**
	 * 회의실 예약 등록
	 * @param meetRes
	 * @return
	 */
	public int insertMeetingReservation(MeetingReservationVO meetRes);

	/**
	 * 회의실 예약 수정
	 * @param meetRes
	 * @return
	 */
	public int updateMeetingReservation(MeetingReservationVO meetRes);

	/**
	 * 회의실 예약 삭제
	 * @param reservationId
	 * @return
	 */
	public int deleteMeetingReservation(MeetingReservationVO meetRes);

	/**
	 * 예약된 회의실인지 조회
	 * @param reservation 회의실Id, 오늘 날짜 정보가 들어있는 vo
	 * @return 조회 결과 없으면 list.size() == 0
	 */
	public List<MeetingReservationVO> selectExistMeetingReservation(MeetingReservationVO reservation);

}
