package kr.or.ddit.email.box.service;

import java.util.List;

import kr.or.ddit.comm.exception.EntityNotFoundException;
import kr.or.ddit.comm.paging.PaginationInfo;
import kr.or.ddit.vo.EmailBoxVO;
import kr.or.ddit.vo.EmailContentVO;

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
 *  2025. 9. 25.     	홍현택	          메일함 서비스 생성
 *  2025. 10.10.		홍현택			받은 이메일 함 목록 조회 retrieveEmailList
 *  2025. 10.15.		홍현택			메일함 별 메일갯수 카운트 readEmailListCount
 *  2025. 10.15. 		홍현택			읽지않은 메일갯수 카운트 readUnreadEmailCount
 *
 * </pre>
 */
public interface EmailBoxService {

    /**
     * 메일함 전체 목록 조회.
     * @return 메일함 목록
     */
    List<EmailBoxVO> readEmailBoxList();

    /**
     * 메일함 단건 조회.
     * @param mailboxId 메일함 식별자
     * @return 메일함
     * @throws  조회 실패 시 EntityNotFoundException
     */
    EmailBoxVO readEmailBox(String mailboxId) throws EntityNotFoundException;

    /**
     * 특정 사용자의 메일함 유형에 따른 이메일 목록을 조회
     * @param userId 사용자 ID
     * @param mailboxTypeCd 메일함 유형 코드 (예: G101 for Inbox)
     * @param paging PaginationInfo 객체
     * @return 이메일 목록 (각 이메일은 Map<String, Object> 형태로 반환)
     */
    List<EmailContentVO> searchEmailList(String userId, String mailboxTypeCd, PaginationInfo<EmailContentVO> paging);

    /**
     * 특정 사용자의 메일함 유형에 따른 이메일 총 개수를 조회
     * @param userId 사용자 ID
     * @param mailboxTypeCd 메일함 유형 코드 (예: G101 for Inbox)
     * @return 이메일 총 개수
     */
    int readEmailListCount(String userId, String mailboxTypeCd, PaginationInfo<EmailContentVO> paging);

    /**
     * 특정 사용자의 메일함 유형에 따른 읽지 않은 이메일 개수를 조회
     * @param userId 사용자 ID
     * @param mailboxTypeCd 메일함 유형 코드 (예: G101 for Inbox)
     * @return 읽지 않은 이메일 개수
     */
    int readUnreadEmailCount(String userId, String mailboxTypeCd);
}
