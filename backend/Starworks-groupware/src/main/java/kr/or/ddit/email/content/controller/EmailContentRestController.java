package kr.or.ddit.email.content.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kr.or.ddit.email.content.service.EmailContentService;
import kr.or.ddit.vo.EmailContentVO;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author 홍현택
 * @since 2025. 9. 26.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 26.     	홍현택	          최초 생성
 *
 * </pre>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/rest")
public class EmailContentRestController {
	private final EmailContentService service;

//	/**
//	 * 메일 전문 단건 조회 RestController
//	 * @return
//	 */
//	@GetMapping
//	public EmailContentVO readEmailContent(String userId){
//		return service.readEmailContent(userId);
//	}
//	/**
//	 * 메일 전문 등록 RestcController
//	 * @param mailboxId
//	 * @return
//	 */
//	@GetMapping
//	public boolean registerEmailContent(EmailContentVO emailContent) {
//		return service.registerEmailContent(emailContent);
//	}

	/**
	 * 관리자 대시보드 전체메일수
	 * @return
	 */
	@GetMapping("/EmailCount")
	public Map<String, Object> readEmailCount() {
	    int count = service.readEmailCount();
	    return Map.of("count", count);
	}

}
