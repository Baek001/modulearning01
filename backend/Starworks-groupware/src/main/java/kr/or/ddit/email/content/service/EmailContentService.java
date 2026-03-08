package kr.or.ddit.email.content.service;

import java.util.List;

import kr.or.ddit.comm.exception.EntityNotFoundException;
import kr.or.ddit.comm.paging.PaginationInfo;
import kr.or.ddit.vo.EmailContentVO;

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
 *  2025. 10.13.		홍현택			메일 상세조회 추가
 *  2025. 10.13.		홍현택			임지저장 메일 조회readDraft
 *  2025. 10.14. 		홍현택			중요 메일 상태 변경 (토글)
 *
 * </pre>
 */
public interface EmailContentService {

    /**
     * 메일 전문 목록 조회(페이지네이션).
     * @param paging 페이징 파라미터(검색조건 포함)
     * @return 목록
     */
    List<EmailContentVO> readEmailContentList(PaginationInfo<EmailContentVO> paging);

    /**
     * 메일 전문 목록 전체 레코드 수.
     * @param paging 페이징 파라미터(검색조건 포함)
     * @return 전체 건수
     */
    int readEmailContentTotalRecord(PaginationInfo<EmailContentVO> paging);

    /**
     * 사용자 기준 메일 전문 단건 조회.
     * @param userId 사용자 아이디
     * @return 메일 전문
     * @throws EntityNotFoundException 조회 실패 시
     */
    EmailContentVO readEmailContent(String userId) throws EntityNotFoundException;

    /**
     * 메일 전문 등록.
     * @param emailContent 등록할 VO
     * @return 성공한 레코드 수
     */
    boolean registerEmailContent(EmailContentVO emailContent);

    /**
     * 메일 상세 조회.
     * 상세 조회 시 해당 메일을 '읽음' 상태로 변경한다.
     * @param emailContId 조회할 메일 ID
     * @param userId 현재 로그인한 사용자 ID
     * @return 메일 상세 정보 (내용, 발신자, 수신자, 첨부파일 포함)
     */
    EmailContentVO readEmailDetail(String emailContId, String userId);

    /**
     * 임시저장된 메일 내용을 조회 (편집용)..
     * @param emailContId 조회할 임시 메일 ID
     * @param userId 현재 로그인한 사용자 ID
     * @return 임시 메일 상세 정보
     */
    EmailContentVO readDraft(String emailContId, String userId);

    /**
     * 메일의 중요 상태를 토글(추가/제거)합니다.
     * @param userId 현재 로그인한 사용자 ID
     * @param emailContId 토글할 메일 ID
     * @return 토글 후 중요 메일 상태 (true: 중요, false: 중요하지 않음)
     */
    boolean toggleImportance(String userId, String emailContId);


    /**
     * 관리자 대시보드 이메일 건수
     * @return
     */
    int readEmailCount();
}
