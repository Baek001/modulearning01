package kr.or.ddit.messenger.read.service;

/**
 *
 * @author 김주민
 * @since 2025. 10. 15.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 15.     	김주민	          최초 생성
 *
 * </pre>
 */
public interface MessengerReadService {

	/**
     * 특정 대화방의 읽지 않은 모든 메시지를 읽음 처리합니다.
     * @param msgrId 채팅방 ID
     * @param userId 현재 사용자 ID
     * @return 성공 시 true
     */
    public boolean markRoomMessagesAsRead(String msgrId, String userId);

}
