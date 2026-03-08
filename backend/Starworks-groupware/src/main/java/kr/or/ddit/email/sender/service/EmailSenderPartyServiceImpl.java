package kr.or.ddit.email.sender.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import kr.or.ddit.comm.exception.EntityNotFoundException;
import kr.or.ddit.comm.paging.PaginationInfo;
import kr.or.ddit.mybatis.mapper.EmailSenderPartyMapper;
import kr.or.ddit.vo.EmailSenderPartyVO;

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
@Service
public class EmailSenderPartyServiceImpl implements EmailSenderPartyService {

	@Autowired
	private EmailSenderPartyMapper mapper;

	/**
	 * 메일 발신자 전체 레코드 출력
	 */
	@Override
	public int readTotalRecord(PaginationInfo<EmailSenderPartyVO> paging) {
		return mapper.selectTotalRecord(paging);
	}

	/**
	 * 메일 발신자 목록 조회
	 */
	@Override
	public List<EmailSenderPartyVO> readEmailSenderPartyList(PaginationInfo<EmailSenderPartyVO> paging) {
		return mapper.selectEmailSenderPartyList(paging);
	}

	/**
	 * 메일 발신자 목록 논페이징
	 */
	@Override
	public List<EmailSenderPartyVO> readEmailSenderPartyListNonPaging() {
		return mapper.selectEmailSenderPartyListNonPaging();
	}

	/**
	 * 메일 발신자 단건으로 조회
	 */
	@Override
	public EmailSenderPartyVO readEmailSenderParty(String userId) throws EntityNotFoundException {
		EmailSenderPartyVO found = mapper.selectEmailSenderParty(userId);
		if (found == null) {
			throw new EntityNotFoundException(userId);
		}
		return found;
	}

	/**
	 * 메일 발신자 등록?
	 */
	@Override
	public boolean registerEmailSenderParty(EmailSenderPartyVO emailsender) {
		return mapper.insertEmailSenderParty(emailsender) > 0;
	}
}


