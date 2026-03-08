package kr.or.ddit.email.receiver.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import kr.or.ddit.comm.exception.EntityNotFoundException;
import kr.or.ddit.mybatis.mapper.EmailReceiverMapper;
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
 *  </pre>
 */
@Service
@RequiredArgsConstructor
public class EmailReceiverServiceImpl implements EmailReceiverService {

	@Autowired
	private EmailReceiverMapper mapper;

	/**
	 *  이메실 수신자 목록을 볼 수 있다.
	 */
	@Override
	public List<EmailReceiverVO> readEmailReceiverList() {
		return mapper.selectEmailReceiverList();
	}

	/**
	 * 이메일 수신자를 조회하고. 없으면 EntityNotFoundException
	 */
	@Override
	public EmailReceiverVO readEmailReceiver(String emailContId) throws EntityNotFoundException {
		EmailReceiverVO found = mapper.selectEmailReceiver(emailContId);
		if (found == null) {
			throw new EntityNotFoundException(emailContId);
		}
		return found;
	}

	/**
	 *	이메일 수신자 등록...
	 */
	@Override
	public boolean registerEmailReceiver(EmailReceiverVO emailReceiver) {
		return mapper.insertEmailReceiver(emailReceiver) > 0;

	}

	/**
	 * 이메일 수신자를 수정..
	 */
	@Override
	public int modifyEmailReceiver(EmailReceiverVO emailReceiver) {
		return mapper.updateEmailMapping(emailReceiver);
	}

}
