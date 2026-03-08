package kr.or.ddit.messenger.room.service;

import java.util.List;

import org.springframework.stereotype.Service;

import kr.or.ddit.comm.exception.EntityNotFoundException;
import kr.or.ddit.mybatis.mapper.MessengerRoomMapper;
import kr.or.ddit.vo.MessengerRoomVO;
import lombok.RequiredArgsConstructor;

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
@Service
@RequiredArgsConstructor
public class MessengerRoomServiceImpl implements MessengerRoomService{

	private final MessengerRoomMapper mapper;
	
	@Override
	public boolean createMessengerRoom(MessengerRoomVO mesRoom) {
		return mapper.insertMessengerRoom(mesRoom) > 0;
	}

	@Override
	public List<MessengerRoomVO> readMessengerRoomList() {
		return mapper.selectMessengerRoomList();
				
	}

	@Override
	public MessengerRoomVO readMessengerRoom(String msgrId) {
		MessengerRoomVO mesRoom = mapper.selectMessengerRoom(msgrId);
		if(mesRoom == null) {
			throw new EntityNotFoundException(mesRoom);
		}
		return mesRoom;
	}

	@Override
	public boolean removeMessengerRoom(String msgrId) {
		return mapper.deleteMessengerRoom(msgrId) > 0;
	}

}
