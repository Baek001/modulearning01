package kr.or.ddit.mybatis.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import kr.or.ddit.vo.EmailReceiverVO;

/**
 *
 * @author 홍현택
 * @since 2025. 9. 25.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 25.     	홍현택	         이메일 매핑 Mapper crud
 *  2025 10. 10. 		홍현택 			임시저장 메일 수정 시 수신자 정보도 수정
 *
 *</pre>
 */
@Mapper
public interface EmailReceiverMapper {

	public int insertEmailMapping(EmailReceiverVO Emailreceive);

	/**
	 * 이메일수신자 생성
	 *
	 * @param Emailmap
	 * @return
	 */
	public int insertEmailReceiver(EmailReceiverVO Emailreceive);

	/**
	 * 이메일수신자 리스트
	 *
	 * @return
	 */
	public List<EmailReceiverVO> selectEmailReceiverList();

	/**
	 * 이메일수신자 단건 조회
	 *
	 * @param emailContId
	 * @return
	 */
	public EmailReceiverVO selectEmailReceiver(String EmailContId);

	/**
	 * 이메일 ID로 모든 수신자 정보 조회
	 *
	 * @param emailContId
	 * @return
	 */
	public java.util.List<kr.or.ddit.vo.EmailReceiverVO> selectReceiversByEmailId(String emailContId);

	/**
	 * 이메일 ID로 모든 수신자 정보 삭제
	 *
	 * @param emailContId
	 * @return
	 */
	public int deleteEmailReceiversByEmailId(String emailContId);

	/**
	 * 이메일수신자 수정
	 *
	 * @param Emailmap
	 * @return
	 */
	public int updateEmailMapping(EmailReceiverVO Emailreceive);

	/**
	 * 여러 이메일 수신자 등록
	 *
	 * @param receiverList
	 * @return
	 */
	public int insertEmailReceivers(List<EmailReceiverVO> receiverList);

}
