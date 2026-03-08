package kr.or.ddit.email.box.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import kr.or.ddit.email.box.service.EmailBoxService;
import kr.or.ddit.vo.EmailBoxVO;
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
 *  -----------	   	-------------    ---------------------------
 *  2025. 9. 26.     	홍현택	          최초 생성
 *
 * </pre>
 */
//@RestController
@RequiredArgsConstructor
@RequestMapping("/rest")
public class EmailBoxRestController {
	private final EmailBoxService service;

	/**
	 * 메일함 목록 조회 RestController
	 * @return
	 */
	@GetMapping
	public List<EmailBoxVO> readEmailBoxList(){
		return service.readEmailBoxList();
	}
	/**
	 * 메일함 하나 조회 .RestcController
	 * @param mailboxId
	 * @return
	 */
	@GetMapping
	public EmailBoxVO readEmailBox(@PathVariable String mailboxId) {
		return service.readEmailBox(mailboxId);
	}
}
