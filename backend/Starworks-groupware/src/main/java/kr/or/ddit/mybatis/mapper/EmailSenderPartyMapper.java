package kr.or.ddit.mybatis.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import kr.or.ddit.comm.paging.PaginationInfo;
import kr.or.ddit.vo.EmailSenderPartyVO;

/**
 *
 * @author 홍현택
 * @since 2025. 9. 25.
 * @see
 *
 *      <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 25.     	홍현택	          최초 생성
 *  2025.10. 10.		홍현택			발신자 등록과 조회를 위한 시그니처 추가 insertEmailSenderParty, selectSenderByEmailId
 *	2025.10. 13. 		홍현택			임시저장 메일 수정 시 존재하는 발신자 삭제 deleteEmailSenderPartyByEmailId
 *
 *      </pre>
 */
@Mapper
public interface EmailSenderPartyMapper {

	/**
	 * EmailSenderPartyMapper 전체 레코드 수 조회.
	 *
	 * @param paging
	 * @return
	 */
	public int selectTotalRecord(PaginationInfo<EmailSenderPartyVO> paging);

	/**
	 * 메일 발신자 정보 등록
	 *
	 * @param senderParty 등록할 VO
	 * @return 성공한 레코드 수
	 */
	public int insertEmailSenderParty(EmailSenderPartyVO senderParty);

	/**
	 * 메일발신자 조회
	 *
	 * @param paging
	 * @return
	 */
	public List<EmailSenderPartyVO> selectEmailSenderPartyList(PaginationInfo<EmailSenderPartyVO> paging);

	/**
	 * 메일 발신자 단건 조회
	 *
	 * @param userId
	 * @return
	 */
	public EmailSenderPartyVO selectEmailSenderParty(String userId);

	/**
	 * 메일발신자 목록 조회. (페이징 x)
	 *
	 * @return 조회 결과 없으면 list.size() == 0
	 */
	public List<EmailSenderPartyVO> selectEmailSenderPartyListNonPaging();

	/**
	 * 이메일 ID로 발신자 정보 조회
	 *
	 * @param emailContId
	 * @return
	 */
	public EmailSenderPartyVO selectSenderByEmailId(String emailContId);

	/**
	 * 이메일 ID로 발신자 정보 삭제
	 *
	 * @param emailContId
	 * @return
	 */
	public int deleteEmailSenderPartyByEmailId(String emailContId);
}
