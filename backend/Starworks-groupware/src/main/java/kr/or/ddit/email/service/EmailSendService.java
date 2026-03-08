package kr.or.ddit.email.service;

import kr.or.ddit.vo.EmailContentVO;

/**
 *
 * @author 홍현택
 * @since 2025. 10. 13.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 13.     	홍현택	          최초 생성
 *  2025. 10. 13.		홍현택 			이메일 발송, 임시저장
 *  2025. 10. 14. 		홍현택			답장, 전달 메서드 추가
 *  2025. 10. 14. 		홍현택			메일 삭제 메서드 추가 deleteEmails deleteAllEmails
 *  2025. 10. 16.		홍현택		휴지통에서 메일 복원 기능 추가restoreEmails
 *
 * </pre>
 */
public interface EmailSendService {
    /**
     * 이메일을 발송하는 메서드
     * @param emailContentVO
     * @return
     */
    public int sendEmail(EmailContentVO emailContentVO);

    /**
     * 이메일을 임시저장한다.
     * @param emailContentVO 임시저장할 메일 내용
     * @return 성공 여부
     */
    public boolean saveDraft(EmailContentVO emailContentVO, String[] recipients);

    /**
     * 답장 이메일을 생성하고 발송합니다.
     * @param originalEmailContId 원본 메일 ID
     * @param currentUserId 현재 사용자 ID
     * @return 성공 여부
     */
    public int replyEmail(String originalEmailContId, EmailContentVO emailContentVO, String[] recipients);

    /**
     * 전달 이메일을 생성하고 발송합니다.
     * @param originalEmailContId 원본 메일 ID
     * @param emailContentVO 전달할 메일 내용
     * @param recipients 수신자 목록
     * @return 성공 여부
     */
    public int forwardEmail(String originalEmailContId, EmailContentVO emailContentVO, String[] recipients);

    /**
     * 선택된 이메일들을 삭제합니다 (휴지통으로 이동 또는 영구 삭제).
     * @param emailContIds 삭제할 이메일 ID 목록
     * @param mailboxTypeCd 현재 메일함 유형 코드 (휴지통 여부 판단용)
     * @param userId 현재 사용자 ID
     * @return 삭제된 이메일 수
     */
    public int deleteEmails(String[] emailContIds, String mailboxTypeCd, String userId);

    /**
     * 특정 메일함의 모든 이메일을 삭제합니다 (휴지통으로 이동 또는 영구 삭제).
     * @param mailboxTypeCd 현재 메일함 유형 코드
     * @param userId 현재 사용자 ID
     * @return 삭제된 이메일 수
     */
    int deleteAllEmails(String mailboxTypeCd, String userId);

    /**
     * 선택된 이메일들을 휴지통에서 복원합니다.
     * @param emailContIds 복원할 이메일 콘텐츠 ID 배열
     * @param userId 현재 로그인한 사용자 ID
     * @return 복원된 이메일 개수
     */
    int restoreEmails(String[] emailContIds, String userId);
}
