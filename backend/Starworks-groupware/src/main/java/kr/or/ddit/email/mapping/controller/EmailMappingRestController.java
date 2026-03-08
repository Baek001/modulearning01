package kr.or.ddit.email.mapping.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import kr.or.ddit.email.mapping.service.EmailMappingService;
import kr.or.ddit.vo.EmailMappingVO;
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
public class EmailMappingRestController {
	private final EmailMappingService service;

	/**
	 * 이메일 매핑 목록 조회 RestController
	 * @return
	 */
	@GetMapping("/mappings")
	public List<EmailMappingVO> readEmailMappingList(String mailboxId){
		return service.readEmailMappingList(mailboxId);
	}
	/**
	 * 이메일 매핑 단건 조회 RestcController
	 * @param mailboxId
	 * @return
	 */
	@GetMapping("/mapping")
	public EmailMappingVO readEmailMapping(
			@RequestParam String mailboxId,
			@RequestParam String emailContId
		) {
		return service.readEmailMapping(mailboxId, emailContId);
	}

	/**
	 * 매일 매핑 수정.
	 * @param emailMapping 수정할 vo
	 * @return
	 */
	@PostMapping("/mapping/update")
	public int modifyEmailMapping(EmailMappingVO emailMapping) {
		return service.modifyEmailMapping(emailMapping);
	}

	/**
	 * 이메일 매핑 등록
	 * @param emailMapping
	 * @return
	 */
	@PostMapping("/mapping/register")
	public boolean registerEmailMapping(EmailMappingVO emailMapping) {
		return service.registerEmailMapping(emailMapping);
	}
}
