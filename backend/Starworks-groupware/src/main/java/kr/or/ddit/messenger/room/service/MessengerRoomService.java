package kr.or.ddit.messenger.room.service;

import java.util.List;

import kr.or.ddit.vo.MessengerRoomVO;

/**
 *
 * @author 윤서현
 * @since 2025. 9. 26.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 26.     	윤서현	          최초 생성
 *
 * </pre>
 */
public interface MessengerRoomService {
	/**
	 * 메신저 대화방 추가
	 * @param mesRoom
	 * @return
	 */
	public boolean createMessengerRoom(MessengerRoomVO mesRoom);
	/**
	 * 메신저 대화방 목록 조회
	 * @return
	 */
	public List<MessengerRoomVO> readMessengerRoomList();
	/**
	 * 메신저 대화방 상세조회
	 * @param msgrId
	 * @return
	 */
	public MessengerRoomVO readMessengerRoom(String msgrId);
	/**
	 * 메신저 대화방 삭제
	 * @param msgrId
	 * @return
	 */
	public boolean removeMessengerRoom(String msgrId);
}
