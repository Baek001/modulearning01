package kr.or.ddit.email.sender.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import kr.or.ddit.email.sender.service.EmailSenderPartyService;
import kr.or.ddit.vo.EmailSenderPartyVO;
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
//@RestController
@RequiredArgsConstructor
@RequestMapping("/rest")
public class EmailSenderPartyrRestController {
	private final EmailSenderPartyService service;
	
	
	/** 메일 발신자 목록 조회
	 * @return
	 */
	@GetMapping
	public List<EmailSenderPartyVO> readEmailSenderPartyListNonPaging(){
		return service.readEmailSenderPartyListNonPaging();
	}

	/** 이메일 발신자 조회
	 * @param userId
	 * @return
	 */
	@GetMapping
	public EmailSenderPartyVO readEmailSenderParty(String userId) {
		return service.readEmailSenderParty(userId);
	}
	

	/**
	 * 이메일 발신자 등록
	 * @param emailsender
	 * @return
	 */
	@GetMapping
	public boolean registerEmailSenderParty(EmailSenderPartyVO emailsender) {
		return service.registerEmailSenderParty(emailsender);
	}
	
}
