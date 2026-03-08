package kr.or.ddit.email.sender.service;

import java.util.List;

import kr.or.ddit.comm.exception.EntityNotFoundException;
import kr.or.ddit.comm.paging.PaginationInfo;
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
public interface EmailSenderPartyService {
	/**
	 * 메일 발신자 전체 레코드 수 조회.
	 * 
	 * @param paging 페이징/검색 조건
	 * @return 전체 건수
	 */
	int readTotalRecord(PaginationInfo<EmailSenderPartyVO> paging);

	/**
	 * 메일 발신자 목록 조회(페이징).
	 * 
	 * @param paging 페이징/검색 조건
	 * @return 목록 (없으면 size==0)
	 */
	List<EmailSenderPartyVO> readEmailSenderPartyList(PaginationInfo<EmailSenderPartyVO> paging);

	/**
	 * 메일 발신자 목록 조회(비페이징).
	 * 
	 * @return 목록 (없으면 size==0)
	 */
	List<EmailSenderPartyVO> readEmailSenderPartyListNonPaging();

	/**
	 * 메일 발신자 단건 조회.
	 * 
	 * @param userId 사용자 ID
	 * @return 발신자 VO
	 * @throws EntityNotFoundException 조회 실패 시
	 */
	EmailSenderPartyVO readEmailSenderParty(String userId) throws EntityNotFoundException;

	/**
	 * 메일 발신자 등록.
	 * 
	 * @param emailsender 등록할 VO
	 * @return 성공한 레코드 수
	 */
	boolean registerEmailSenderParty(EmailSenderPartyVO emailsender);
}

	

