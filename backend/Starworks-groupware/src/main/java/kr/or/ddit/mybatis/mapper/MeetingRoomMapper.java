package kr.or.ddit.mybatis.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import kr.or.ddit.vo.MeetingRoomVO;

/**
 *
 * @author 윤서현
 * @since 2025. 10. 17.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           	   수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 17.     	윤서현	          최초 생성
 *  2025. 10. 24.		임가영			  회의실 추가 Mapper 생성
 *
 * </pre>
 */
@Mapper
public interface MeetingRoomMapper {

	/**
	 * 회의실 상세조회
	 * @param roomId
	 * @return
	 */
	public MeetingRoomVO selectMeetingRoom(String roomId);

	/**
	 * 회의실 리스트 조회
	 * @return
	 */
	public List<MeetingRoomVO> selectMeetingRoomList();

	/**
	 * 회의실 추가
	 * @param room 추가할 회의실 정보를 담은 vo
	 * @return 성공한 레코드 수
	 */
	public int insertMeetingRoom(MeetingRoomVO room);

	/**
	 * 회의실 정보 수정
	 * @param room 수정할 회의실 정보를 담은 vo
	 * @return 성공한 레코드 수
	 */
	public int updateMeetingRoom(MeetingRoomVO room);

	/**
	 * 회의실 상태 변경
	 * @param room
	 * @return
	 */
	public int updateMeetingRoomStatus(MeetingRoomVO room);


}
