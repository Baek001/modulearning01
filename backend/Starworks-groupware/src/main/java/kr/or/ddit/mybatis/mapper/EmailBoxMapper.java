package kr.or.ddit.mybatis.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import kr.or.ddit.comm.paging.PaginationInfo;
import kr.or.ddit.vo.EmailBoxVO;
import kr.or.ddit.vo.EmailContentVO;
/**
 *
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
 *  2025. 9. 25.     	홍현택	          메일함 mapper 작성
 *	2025. 10.10.		홍현택			메일함 목록 조회 메서드 추가
 *  2025. 10.15.		홍현택		사용자의 메일함 유형에 따른 이메일 총 개수를 조회 findMailboxIdByUserIdAndType
 *  2025. 10.15. 		홍현택		사용자의 메일함 유형에 따른 읽지 않은 이메일 개수를 조회 selectUnreadEmailCount
 *
 *
 * </pre>
*/

@Mapper

public interface EmailBoxMapper {

	/**
	 * 메일함 전부를 출력하는 메서드
	 * @return
	 *
	 */
	public List<EmailBoxVO> selectEmailBoxList();

	/**
	 *
	 * 메일함 하나를 출력하는 메서드
	 *
	 * @param mailboxId
	 *
	 * @return
	 *
	 */

	public EmailBoxVO selectEmailBox(String mailboxId);

	/**
	 *
	 * 특정 사용자의 메일함 유형에 따른 이메일 목록을 조회
	 *
	 * @param userId        사용자 ID
	 *
	 * @param mailboxTypeCd 메일함 유형 코드 (예: G101 for Inbox)
	 * @param paging        PaginationInfo 객체
	 * @return 이메일 목록 (EmailContentVO 형태로 반환)
	 *
	 */

	public List<EmailContentVO> selectEmailList(@Param("userId") String userId, @Param("mailboxTypeCd") String mailboxTypeCd,
			@Param("paging") PaginationInfo<EmailContentVO> paging);



	/**
	 * 사용자 ID와 메일함 타입으로 메일함 ID 조회
	 *
	 * @param params Map containing userId and mailboxTypeCd
	 * @return mailboxId
	 */
	public String findMailboxIdByUserIdAndType(java.util.Map<String, Object> params);

	/**
	 * 사용자의 메일함 유형에 따른 이메일 총 개수를 조회
	 *
	 * @param userId        사용자 ID
	 * @param mailboxTypeCd 메일함 유형 코드 (예: G101 for Inbox)
	 * @return 이메일 총 개수
	 */
	public int selectEmailListCount(@Param("userId") String userId, @Param("mailboxTypeCd") String mailboxTypeCd,
			@Param("paging") PaginationInfo<EmailContentVO> paging);
	/**
	 * 사용자의 메일함 유형에 따른 읽지 않은 이메일 개수를 조회
	 *
	 * @param userId        사용자 ID
	 * @param mailboxTypeCd 메일함 유형 코드 (예: G101 for Inbox)
	 * @return 읽지 않은 이메일 개수
	 */
	public int selectUnreadEmailCount(@Param("userId") String userId, @Param("mailboxTypeCd") String mailboxTypeCd);
}
