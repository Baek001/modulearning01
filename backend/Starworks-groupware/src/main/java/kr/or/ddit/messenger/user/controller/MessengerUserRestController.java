package kr.or.ddit.messenger.user.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kr.or.ddit.messenger.user.service.MessengerUserService;
import kr.or.ddit.vo.MessengerUserVO;
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
@RequestMapping("/rest/chat-user")
@RequiredArgsConstructor
public class MessengerUserRestController {
	private final MessengerUserService service;
	
	/**
	 * 메신저 참여자 목록조회. RestController
	 * @return
	 */
	@GetMapping
	public List<MessengerUserVO> readMessengerUserList(){
		return service.readMessengerUserList();
	}
	
	/**
	 * 메신저 참여자 상세조회. RestController
	 * @param userId
	 * @return
	 */
	@GetMapping("/{userId}")
	public MessengerUserVO readMessengerUser(@PathVariable String userId) {
		return service.readMessengerUser(userId);
	}
	
}
