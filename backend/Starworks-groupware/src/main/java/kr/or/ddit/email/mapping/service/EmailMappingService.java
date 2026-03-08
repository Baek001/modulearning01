package kr.or.ddit.email.mapping.service;

import java.util.List;

import kr.or.ddit.comm.exception.EntityNotFoundException;
import kr.or.ddit.vo.EmailMappingVO;

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
 *  2025.10. 13.		홍현택			상세조회 전용 매핑 등록
 *
 *      </pre>
 */

public interface EmailMappingService {


	/**
	 * 이메일 매핑 목록 조회.
	 *
	 * @param mailboxId
	 * @return 매핑 목록
	 */
	List<EmailMappingVO> readEmailMappingList(String mailboxId);

	/**
	 * 메일 매핑 단건 조회.
	 *
	 * @param mailboxId 메일함 식별자
	 * @param emailContId 이메일 콘텐츠 식별자
	 * @return 매핑 VO
	 * @throws 조회 실패 시 EntityNotFoundException
	 */
	EmailMappingVO readEmailMapping(String mailboxId, String emailContId) throws EntityNotFoundException;

	/**
	 * 메일 매핑 수정.
	 *
	 * @param emailMapping 수정할 VO
	 * @return
	 */
	int modifyEmailMapping(EmailMappingVO emailMapping);

	/**
	 * 이메일 매핑 등록.
	 *
	 * @param emailMapping 등록할 VO
	 * @return성공한 레코드 수
	 */
	boolean registerEmailMapping(EmailMappingVO emailMapping);








}
