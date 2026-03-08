package kr.or.ddit.email.mapping.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import kr.or.ddit.comm.exception.EntityNotFoundException;
import kr.or.ddit.mybatis.mapper.EmailMappingMapper;
import kr.or.ddit.vo.EmailMappingVO;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author 홍현택
 * @since 2025. 9. 26.
 * @see
 *
 *      <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 26.     	홍현택	          최초 생성
 *
 *      </pre>
 */
@Service
@RequiredArgsConstructor
public class EmailMappingServiceImpl implements EmailMappingService {

	private final EmailMappingMapper mapper;

	/**
	 * 이메일 매핑 목록 조회(by mailboxId)
	 */
	@Override
	public List<EmailMappingVO> readEmailMappingList(String mailboxId) {
		return mapper.selectEmailMappingList();
	}

	/**
	 * 이메일 매핑 단건 조회 (없으면 EntityNotFoundException)
	 */
	@Override
	public EmailMappingVO readEmailMapping(String mailboxId, String emailContId) throws EntityNotFoundException {
	    Map<String, Object> params = new HashMap<>();
	    params.put("mailboxId", mailboxId);
	    params.put("emailContId", emailContId);
	    EmailMappingVO vo = mapper.selectEmailMapping(params);
		if (vo == null) {
			throw new EntityNotFoundException(String.format("%s, %s 에 해당하는 메일 없음", mailboxId, emailContId));
		}
		return vo;
	}

	/**
	 * 이메일 매핑 수정
	 */
	@Override
	public int modifyEmailMapping(EmailMappingVO emailMapping) {
		return mapper.updateEmailMapping(emailMapping);
	}

	/**
	 * 이메일 매핑 등록 (성공 여부 반환)
	 */
	@Override
	public boolean registerEmailMapping(EmailMappingVO emailMapping) {
		return mapper.insertEmailMapping(emailMapping) > 0;
	}
}
