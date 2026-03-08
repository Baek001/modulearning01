package kr.or.ddit.messenger.content.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kr.or.ddit.messenger.content.service.MessengerContentService;
import kr.or.ddit.vo.MessengerContentVO;
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
@RequestMapping("/rest/chat-content")
@RequiredArgsConstructor
public class MessengerContentRestController {
	
	private final MessengerContentService service;
	
	/**
	 * 메신저 대화 목록 조회. RestController
	 * @return
	 */
	@GetMapping
	public List<MessengerContentVO> readMessengerContentList(){
		return service.readMessengerContentList();
	}
	
}
