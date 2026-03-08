package kr.or.ddit.meeting.service;

import java.util.List;

import kr.or.ddit.vo.MeetingRoomVO;

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
public interface MeetingRoomService {
	/**
	 * 전체 회의실 조회
	 * @return
	 */
	public List<MeetingRoomVO> readMeetingRoomList();

	/**
	 * 회의실 상세조회
	 * @param roomId
	 * @return
	 */
	public MeetingRoomVO readMeetingRoom(String roomId);

	/**
	 * 회의실 추가
	 * @param room 추가할 회의실 정보를 담은 vo
	 * @return 성공하면 true, 실패하면 false
	 */
	public boolean createMeetingRoom(MeetingRoomVO room);

	/**
	 * 회의실 정보수정
	 * @param room 수정할 회의실 정보를 담은 vo
	 * @return
	 */
	public boolean modifyMeetingRoom(MeetingRoomVO room);

	/**
	 * 회의실 상태 업데이트
	 * @param room 닫음/열람 여부 또는 회의실 삭제 여부를 담은 vo
	 * @return
	 */
	public boolean openAndCloseMeetingRoom(MeetingRoomVO room);
}
