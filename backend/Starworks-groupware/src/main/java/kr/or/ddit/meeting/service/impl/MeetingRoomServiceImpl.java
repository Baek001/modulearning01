package kr.or.ddit.meeting.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import kr.or.ddit.comm.exception.EntityNotFoundException;
import kr.or.ddit.meeting.service.MeetingRoomService;
import kr.or.ddit.mybatis.mapper.JobGradeMapper;
import kr.or.ddit.mybatis.mapper.MeetingReservationMapper;
import kr.or.ddit.mybatis.mapper.MeetingRoomMapper;
import kr.or.ddit.vo.MeetingReservationVO;
import kr.or.ddit.vo.MeetingRoomVO;
import lombok.RequiredArgsConstructor;

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
@Service
@RequiredArgsConstructor
public class MeetingRoomServiceImpl implements MeetingRoomService{

	private final MeetingRoomMapper mapper;
	private final MeetingReservationMapper reservationMapper;

	@Override
	public List<MeetingRoomVO> readMeetingRoomList() {
		return mapper.selectMeetingRoomList();
	}

	/**
	 * 회의실 단건 조회
	 */
	@Override
	public MeetingRoomVO readMeetingRoom(String roomId) {
		MeetingRoomVO meetingRoom = mapper.selectMeetingRoom(roomId);
		if(meetingRoom == null) {
			throw new EntityNotFoundException(meetingRoom);
		}
		return meetingRoom;
	}

	/**
	 * 회의실 추가
	 */
	@Override
	public boolean createMeetingRoom(MeetingRoomVO room) {
		return mapper.insertMeetingRoom(room) > 0;
	}

	/**
	 * 회의실 정보 수정
	 */
	@Override
	public boolean modifyMeetingRoom(MeetingRoomVO room) {
		return mapper.updateMeetingRoom(room) > 0;
	}

	/**
	 * 회의실 상태 업데이트
	 * @param room 닫음/열람 여부 또는 회의실 삭제 여부를 담은 vo
	 * @return
	 */
	public boolean openAndCloseMeetingRoom(MeetingRoomVO room) {
		// 만약 회의실 삭제라면 회의 예약까지 삭제한 후 삭제
//		if (room.getDelYn() != null && room.getDelYn().equals("Y")) {
//			MeetingReservationVO reservationVO = new MeetingReservationVO();
//			reservationVO.setRoomId(room.getRoomId());
//			List<MeetingReservationVO> reservationList = reservationMapper.selectMeetingReservationList(reservationVO);
//
//			// 만약 삭제할 회의실에 예약 내역이 있다면
//			if(!reservationList.isEmpty()) {
//				for(MeetingReservationVO reservation : reservationList) {
//					reservationMapper.deleteMeetingReservation(reservation.getReservationId());
//				}
//			}
//		}

		return mapper.updateMeetingRoomStatus(room) > 0;
	}

}
