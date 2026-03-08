package kr.or.ddit.messenger.read.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.mybatis.mapper.MessengerReadMapper;
import lombok.RequiredArgsConstructor;

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
@Service
@RequiredArgsConstructor
public class MessengerReadServiceImpl implements MessengerReadService{

	private MessengerReadMapper readMapper;

	/**
	 * 메시지 읽음 처리
	 *
	 * @param msgrId 읽음 처리할 채팅방 ID
	 * @param userId 읽음 처리를 수행하는 사용자 ID
	 */
	@Override
	@Transactional
	public boolean markRoomMessagesAsRead(String msgrId, String userId) {
		readMapper.insertReadRecords(msgrId, userId);
		return true;
	}

}
