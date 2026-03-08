package kr.or.ddit.email.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import kr.or.ddit.email.service.EmailSendService;
import kr.or.ddit.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author 홍현택
 * @since 2025. 10. 14.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 14.     	홍현택	          최초 생성 (메일 삭제 기능 분리)
 *  2025. 10. 16		홍현택		   휴지통에서 메일 복원 기능 추가
 * </pre>
 */
@Controller
@RequiredArgsConstructor // final 필드(서비스) 생성자 주입
@RequestMapping("/mail") // 본 컨트롤러의 공통 URL prefix
@Slf4j
public class EmailRemoveController {

    // 메일 삭제/이동 트랜잭션을 수행하는 도메인 서비스
    private final EmailSendService emailSendService;

    /**
     * 선택된 이메일들을 삭제합니다 (휴지통으로 이동 또는 영구 삭제).
     *
     * <p>요청 바디 예시(JSON):
     * <pre>
     * {
     *   "emailContIds": ["EC000001", "EC000002"], // 필수: 삭제 대상 EMAIL_CONT_ID 배열
     *   "mailboxTypeCd": "G105"                   // 필수: 이동/처리할 메일함 유형(정책에 따라 휴지통/영구삭제 판단)
     * }
     * </pre>
     *
     * @param payload         요청 JSON 맵 (emailContIds, mailboxTypeCd)
     * @param authentication  현재 로그인한 사용자 인증 컨텍스트 (Spring Security)
     * @return 처리 결과 맵(JSON) - {status: success|fail|error, message: ...}
     */
    @PostMapping("/deleteSelected")
    @ResponseBody // Map을 JSON으로 직렬화하여 응답
    public Map<String, Object> deleteSelectedEmails(
            @RequestBody Map<String, Object> payload, // 요청 본문을 키-값 맵으로 바인딩
            Authentication authentication               // SecurityContext의 인증 객체
    ) {
        Map<String, Object> result = new HashMap<>(); // 응답 페이로드 (JSON 직렬화 대상)
        try {
            // (1) 보안 컨텍스트에서 사용자 ID 추출
            //     - CustomUserDetails는 프로젝트의 사용자 principal 구현체
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            String userId = userDetails.getRealUser().getUserId(); // 실제 사번/유저ID

            // (2) 요청 파라미터 추출 및 기본 검증
            //     - emailContIds: 삭제 대상 콘텐츠 ID 목록
            @SuppressWarnings("unchecked")
            List<String> emailContIdsList = (List<String>) payload.get("emailContIds");
            String[] emailContIds = (emailContIdsList != null) ? emailContIdsList.toArray(new String[0]) : null;

            //     - mailboxTypeCd: 어떤 메일함 기준으로 처리할지(휴지통/보낸/받은 등)
            String mailboxTypeCd = (String) payload.get("mailboxTypeCd");

            //     - 필수 파라미터 체크(비었거나 null이면 실패 반환)
            if (emailContIds == null || emailContIds.length == 0 || mailboxTypeCd == null) {
                result.put("status", "fail");
                result.put("message", "필수 파라미터가 누락되었습니다.");
                return result; // 빠른 반환
            }

            // (3) 도메인 서비스 호출
            //     - 서비스에서 사용자 범위, 메일함 유형 정책, 매핑/소프트/하드 삭제 등을 캡슐화하여 처리
            int deletedCount = emailSendService.deleteEmails(emailContIds, mailboxTypeCd, userId);

            // (4) 처리 결과에 따른 응답 메시지 구성
            if (deletedCount > 0) {
                result.put("status", "success");
                result.put("message", deletedCount + "개의 이메일이 성공적으로 삭제되었습니다.");
            } else {
                // 도메인/쿼리 상 삭제 대상이 없거나, 처리 중 예외 없이 0건일 때
                result.put("status", "fail");
                result.put("message", "삭제할 이메일이 없거나 처리 중 오류가 발생했습니다.");
            }
        } catch (Exception e) {
            // (5) 예외 처리: 운영 로그로 남기고, 클라이언트에는 일반화된 메시지 반환
            log.error("선택 이메일 삭제 중 오류 발생", e);
            result.put("status", "error");
            result.put("message", "서버 오류가 발생했습니다.");
        }
        return result; // JSON 응답
    }

    /**
     * 특정 메일함의 모든 이메일을 삭제합니다 (휴지통으로 이동 또는 영구 삭제).
     *
     * <p>요청 바디 예시(JSON):
     * <pre>
     * {
     *   "mailboxTypeCd": "G101" // 필수: 대상 보관함 유형 코드
     * }
     * </pre>
     *
     * @param payload         요청 JSON 맵 (mailboxTypeCd)
     * @param authentication  현재 로그인한 사용자 정보
     * @return 처리 결과 맵(JSON) - {status: success|fail|error, message: ...}
     */
    @PostMapping("/deleteAll")
    @ResponseBody // Map을 JSON으로 직렬화하여 응답
    public Map<String, Object> deleteAllEmails(
            @RequestBody Map<String, Object> payload, // 요청 본문 바인딩
            Authentication authentication               // 인증 컨텍스트
    ) {
        Map<String, Object> result = new HashMap<>(); // 응답 맵
        try {
            // (1) 사용자 식별
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            String userId = userDetails.getRealUser().getUserId();

            // (2) 파라미터 추출/검증
            String mailboxTypeCd = (String) payload.get("mailboxTypeCd");
            if (mailboxTypeCd == null) {
                result.put("status", "fail");
                result.put("message", "필수 파라미터가 누락되었습니다.");
                return result;
            }

            // (3) 서비스 호출: 사용자(userId)의 특정 보관함 유형에 속한 모든 매핑/콘텐츠를 정책에 맞게 삭제/이동
            int deletedCount = emailSendService.deleteAllEmails(mailboxTypeCd, userId);

            // (4) 결과 반환
            if (deletedCount > 0) {
                result.put("status", "success");
                result.put("message", deletedCount + "개의 이메일이 성공적으로 삭제되었습니다.");
            } else {
                result.put("status", "fail");
                result.put("message", "삭제할 이메일이 없거나 처리 중 오류가 발생했습니다.");
            }
        } catch (Exception e) {
            // (5) 예외 로깅 및 일반화 응답
            log.error("전체 이메일 삭제 중 오류 발생", e);
            result.put("status", "error");
            result.put("message", "서버 오류가 발생했습니다.");
        }
        return result; // JSON 응답
    }

    /**
     * 선택된 이메일들을 휴지통에서 복원합니다.
     *
     * <p>요청 바디 예시(JSON):
     * <pre>
     * {
     *   "emailContIds": ["EC000001", "EC000002"] // 필수: 복원 대상 EMAIL_CONT_ID 배열
     * }
     * </pre>
     *
     * @param payload         요청 JSON 맵 (emailContIds)
     * @param authentication  현재 로그인한 사용자 인증 컨텍스트 (Spring Security)
     * @return 처리 결과 맵(JSON) - {status: success|fail|error, message: ...}
     */
    @PostMapping("/restoreSelected")
    @ResponseBody
    public Map<String, Object> restoreSelectedEmails(
            @RequestBody Map<String, Object> payload,
            Authentication authentication
    ) {
        Map<String, Object> result = new HashMap<>();
        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            String userId = userDetails.getRealUser().getUserId();

            @SuppressWarnings("unchecked")
            List<String> emailContIdsList = (List<String>) payload.get("emailContIds");
            String[] emailContIds = (emailContIdsList != null) ? emailContIdsList.toArray(new String[0]) : null;

            if (emailContIds == null || emailContIds.length == 0) {
                result.put("status", "fail");
                result.put("message", "복원할 이메일이 선택되지 않았습니다.");
                return result;
            }

            int restoredCount = emailSendService.restoreEmails(emailContIds, userId);

            if (restoredCount > 0) {
                result.put("status", "success");
                result.put("message", restoredCount + "개의 이메일이 성공적으로 복원되었습니다.");
            } else {
                result.put("status", "fail");
                result.put("message", "복원할 이메일이 없거나 처리 중 오류가 발생했습니다.");
            }
        } catch (Exception e) {
            log.error("선택 이메일 복원 중 오류 발생", e);
            result.put("status", "error");
            result.put("message", "서버 오류가 발생했습니다.");
        }
        return result;
    }
}
