package kr.or.ddit.email.receiver.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import kr.or.ddit.email.receiver.service.EmailReceiverService;
import kr.or.ddit.vo.EmailReceiverVO;
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
public class EmailReceiverRestController {
	private final EmailReceiverService service;
	
	/**
	 *  이메일 수신자 전체 목록 조회. RestController
	 * @return
	 */
	@GetMapping
	public List<EmailReceiverVO> readEmailReceiverList(){
		return service.readEmailReceiverList();
	}
	/**
	 * 이메일 수신자 단건 조회 RestcController
	 * @param emailContId
	 * @return
	 */
	@GetMapping
	public EmailReceiverVO readEmailReceiver(String emailContId) {
		return service.readEmailReceiver(emailContId);
	}
	
	/**
	 * 이메일 수신자 등록
	 * @param emailReceiver
	 * @return
	 */
	@GetMapping
	public boolean registerEmailReceiver(EmailReceiverVO emailReceiver) {
		return service.registerEmailReceiver(emailReceiver);
	}
	
	/**
	 * 이메일 수신자 수정
	 * @param emailReceiver
	 * @return
	 */
	@GetMapping
	public int modifyEmailReceiver(EmailReceiverVO emailReceiver) {
		return service.modifyEmailReceiver(emailReceiver);
	}
}
