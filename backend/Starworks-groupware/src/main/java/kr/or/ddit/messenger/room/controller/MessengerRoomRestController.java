package kr.or.ddit.messenger.room.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kr.or.ddit.messenger.room.service.MessengerRoomService;
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
@RestController
@RequestMapping("/rest/chat-room")
@RequiredArgsConstructor
public class MessengerRoomRestController {

	private MessengerRoomService service;
	
	/**
	 * 메신저 대화방 목록 조회. RestController
	 * @return
	 */
	@GetMapping
	public List<MessengerRoomVO> readMessengerRoomList(){
		return service.readMessengerRoomList();
	}
	
	/**
	 * 메신저 대화방 상세조회. RestController
	 * @param msgrId
	 * @return
	 */
	@GetMapping("/{msgrId}")
	public MessengerRoomVO readMessengerRoom(@PathVariable String msgrId) {
		return service.readMessengerRoom(msgrId);
	}
	
}
