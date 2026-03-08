package kr.or.ddit.email.receiver.service;

import java.util.List;

import kr.or.ddit.comm.exception.EntityNotFoundException;
import kr.or.ddit.vo.EmailReceiverVO;

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
 *  </pre>
 */
public interface EmailReceiverService {

	/**
	 * 이메일 수신자 전체 목록 조회.
	 * 
	 * @return 수신자 목록
	 */
	List<EmailReceiverVO> readEmailReceiverList();

	/**
	 * 이메일 수신자 단건 조회.
	 * 
	 * @param emailContId 메일 전문 ID
	 * @return VO
	 * @throws EntityNotFoundException 조회 실패 시
	 */
	EmailReceiverVO readEmailReceiver(String emailContId) throws EntityNotFoundException;

	/**
	 * 이메일 수신자 등록.
	 * 
	 * @param emailReceiver 등록할 VO
	 * @return 성공한 레코드 수
	 */
	boolean registerEmailReceiver(EmailReceiverVO emailReceiver);

	/**
	 * 이메일 수신자 수정.
	 * 
	 * @param emailReceiver 수정할 VO
	 * @return 성공한 레코드 수
	 */
	int modifyEmailReceiver(EmailReceiverVO emailReceiver);
}
