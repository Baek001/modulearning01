package kr.or.ddit.mybatis.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import kr.or.ddit.vo.EmailMappingVO;

/**
 *
 * @author 홍현택
 * @since 2025. 9. 25.
 * @see
 *
 *  <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 25.     	홍현택	         이메일 매핑 Mapper crud
 *  2025.10. 10.		홍현택			이메일 읽음상태 변경 updateReadStatus
 *  2025.10. 13.		홍현택			임시저장 메일 수정 시 매핑정보도 수정.updateEmailMapping
 *  2025.10. 14.		홍현택			특정 이메일 매핑정보 삭제
 *
 * </pre>
 */
@Mapper
public interface EmailMappingMapper {

	/**
	 * 이매일 매핑 생성
	 *
	 * @param Emailmaping
	 * @return
	 */
	public int insertEmailMapping(EmailMappingVO Emailmap);

	/**
	 * 이메일 매핑 리스트
	 *
	 * @return
	 */
	public List<EmailMappingVO> selectEmailMappingList();

	/**
	 * 이매일 매핑 단건 조회
	 *
	 * @param mailboxId
	 * @return
	 */
	public EmailMappingVO selectEmailMapping(Map<String, Object> params);

	/**
	 * 이메일 매핑 수정
	 *
	 * @param Emailmaping
	 * @return
	 */
	public int updateEmailMapping(EmailMappingVO Emailmap);

	/**
	 * 메일 읽음 상태 변경
	 *
	 * @param params (emailContId, userId)
	 * @return
	 */
	public int updateReadStatus(java.util.Map<String, Object> params);

//===============================삭제 기능=============================================================

	/**
	 * 이메일 ID로 모든 매핑 정보 삭제
	 *
	 * @param emailContId
	 * @return
	 */
	public int deleteEmailMappingsByEmailId(String emailContId);

	/**
	 * 특정 이메일 매핑 정보 삭제 (mailboxId, emailContId 기반)
	 * @param mapping
	 * @return
	 */
	public int deleteEmailMapping(EmailMappingVO mapping);

	/**
	 * 특정 이메일의 메일함 ID를 업데이트합니다 (소프트 삭제).
	 * @param params (emailContId, userId, newMailboxId)
	 * @return 업데이트된 행 수
	 */
	public int updateMailboxIdForEmail(Map<String, Object> params);

	/**
	 * 특정 이메일 매핑의 DEL_YN 상태를 'Y'로 업데이트합니다 (영구 삭제).
	 * @param params (emailContId, userId)
	 * @return 업데이트된 행 수
	 */
	public int updateEmailMappingDelYn(Map<String, Object> params);
	/**
	 * 특정 이메일 콘텐츠 ID와 사용자 ID에 해당하는 매핑 정보를 삭제합니다.
	 * @param params (emailContId, userId)
	 * @return 삭제된 행 수
	 */
	public int deleteEmailMappingByEmailContIdAndUserId(Map<String, Object> params);

// ======================= 메일함 조회 기능 ========================================================

	/**
	 * 특정 이메일 콘텐츠 ID와 사용자 ID에 해당하는 단일 매핑 정보를 조회합니다.
	 * @param params (emailContId, userId)
	 * @return 해당 조건에 맞는 EmailMappingVO (없으면 null)
	 */
	public EmailMappingVO selectEmailMappingByEmailContIdAndUserIdAndMailboxTypeCd(Map<String, Object> params);
	/**
	 * 특정 이메일 콘텐츠 ID에 대한 모든 매핑 정보의 개수를 조회합니다.
	 * @param emailContId 이메일 콘텐츠 ID
	 * @return 해당 이메일의 매핑 개수
	 */
	public int selectEmailMappingsCountByEmailContId(String emailContId);

	/**
	 * 특정 사용자 및 메일함 유형에 해당하는 모든 이메일 매핑 정보를 조회합니다.
	 * @param params (userId, mailboxTypeCd)
	 * @return 해당 조건에 맞는 EmailMappingVO 목록
	 */
	public List<EmailMappingVO> selectEmailMappingsByMailboxTypeCdAndUserId(Map<String, Object> params);}
