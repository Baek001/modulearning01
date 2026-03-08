package kr.or.ddit.approval.controller;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import kr.or.ddit.approval.document.service.AuthorizationDocumentService;
import kr.or.ddit.approval.line.service.AuthorizationLineService;
import kr.or.ddit.approval.otp.service.OtpService;
import kr.or.ddit.vo.AuthorizationLineVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author 홍현택
 * @since 2025. 10. 21.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 21.     	홍현택	          전자 결재 관련 메소드 정리
 *
 *
 * </pre>
 */
@Slf4j
@Controller
@RequestMapping("/approval")
@RequiredArgsConstructor
public class ApprovalActionController {

    private final AuthorizationLineService lineService;
    private final AuthorizationDocumentService docService;
    private final OtpService otpService; // OTP 서비스 주입

    /**
     * OTP 코드의 유효성을 검증하는 메소드
     * @param authentication
     * @param otpCode
     * @return OTP가 유효하거나, 설정되지 않은 경우 true
     */
    private boolean isOtpValid(Authentication authentication, Integer otpCode) {
        String userId = authentication.getName();
        String secretKey = otpService.getUserOtpSecret(userId);

        // OTP가 설정된 사용자인 경우에만 검증
        if (StringUtils.isNotBlank(secretKey)) {
            if (otpCode == null || !otpService.validateOtp(secretKey, otpCode)) {
                return false; // 코드가 없거나 유효하지 않음
            }
        }
        return true; // OTP가 설정되지 않았거나 코드가 유효함
    }

    private boolean isAdmin(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority ->
                        "ROLE_ADMIN".equalsIgnoreCase(authority)
                        || "ADMIN".equalsIgnoreCase(authority)
                );
    }

    @PostMapping("/approve")
    public String approve(
            @RequestParam String docId,
            @RequestParam int lineSqn,
            @RequestParam int currentSeq,
            @RequestParam(required = false) String opinion,
            @RequestParam(required = false) String signFileId,
            @RequestParam(required = false) String htmlData,
            @RequestParam(required = false) Integer otpCode, // OTP 코드 파라미터 추가
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        // OTP 검증
        if (!isOtpValid(authentication, otpCode)) {
            redirectAttributes.addFlashAttribute("message", "OTP 코드가 유효하지 않습니다.");
            redirectAttributes.addFlashAttribute("messageType", "danger");
            redirectAttributes.addFlashAttribute("isOtpError", true); // OTP 오류 플래그
            return "redirect:/approval/detail/" + docId;
        }

        try {
            lineService.modifyApproveAndUpdateStatus(docId, lineSqn, currentSeq, opinion, signFileId, htmlData);
            redirectAttributes.addFlashAttribute("message", "승인되었습니다.");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("message", e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "warning");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "승인 처리 중 오류가 발생했습니다.");
            redirectAttributes.addFlashAttribute("messageType", "danger");
        }

        return "redirect:/approval/main";
    }

    @PostMapping("/delegate")
    public String delegate(
            @RequestParam String docId,
            @RequestParam int lineSqn,
            @RequestParam(required = false) String opinion,
            @RequestParam(required = false) String signFileId,
            @RequestParam(required = false) String htmlData,
            @RequestParam(required = false) Integer otpCode, // OTP 코드 파라미터 추가
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        // OTP 검증
        if (!isOtpValid(authentication, otpCode)) {
            redirectAttributes.addFlashAttribute("message", "OTP 코드가 유효하지 않습니다.");
            redirectAttributes.addFlashAttribute("messageType", "danger");
            redirectAttributes.addFlashAttribute("isOtpError", true); // OTP 오류 플래그
            return "redirect:/approval/detail/" + docId;
        }

        String loginId = authentication.getName();

        AuthorizationLineVO line = new AuthorizationLineVO();
        line.setAtrzDocId(docId);
        line.setAtrzLineSqn(lineSqn);
        line.setAtrzApprUserId(loginId);
        line.setAtrzOpnn(opinion);
        line.setSignFileId(signFileId);

        try {
            lineService.delegateApproval(line, htmlData);
            redirectAttributes.addFlashAttribute("message", "전결 처리되었습니다.");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("message", e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "warning");
        } catch (Exception e) {
            log.error("delegate error", e);
            redirectAttributes.addFlashAttribute("message", "전결 처리 중 오류가 발생했습니다.");
            redirectAttributes.addFlashAttribute("messageType", "danger");
        }

        return "redirect:/approval/main";
    }

    @PostMapping("/retract")
    public String retract(
            @RequestParam String docId,
            RedirectAttributes redirectAttributes
    ) {
        try {
            int affectedRows = docService.updateDocumentStatus(docId, "A205");
            if (affectedRows > 0) {
                redirectAttributes.addFlashAttribute("message", "문서를 회수했습니다.");
                redirectAttributes.addFlashAttribute("messageType", "success");
            } else {
                redirectAttributes.addFlashAttribute("message", "문서 회수에 실패했습니다.");
                redirectAttributes.addFlashAttribute("messageType", "danger");
            }
        } catch (Exception e) {
            log.error("retract error for docId: {}", docId, e);
            redirectAttributes.addFlashAttribute("message", "문서 회수 중 오류가 발생했습니다.");
            redirectAttributes.addFlashAttribute("messageType", "danger");
        }

        return "redirect:/approval/main";
    }

    @PostMapping("/reject")
    public String reject(
            @RequestParam String docId,
            @RequestParam int lineSqn,
            @RequestParam String opinion,
            @RequestParam(required = false) Integer otpCode, // OTP 코드 파라미터 추가
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        // OTP 검증
        if (!isOtpValid(authentication, otpCode)) {
            redirectAttributes.addFlashAttribute("message", "OTP 코드가 유효하지 않습니다.");
            redirectAttributes.addFlashAttribute("messageType", "danger");
            redirectAttributes.addFlashAttribute("isOtpError", true); // OTP 오류 플래그
            return "redirect:/approval/detail/" + docId;
        }

        try {
            lineService.processRejection(docId, lineSqn, opinion);
            redirectAttributes.addFlashAttribute("message", "반려되었습니다.");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (IllegalStateException e) {
            log.warn("Reject failed for docId: {}, lineSqn: {}. Reason: {}", docId, lineSqn, e.getMessage());
            redirectAttributes.addFlashAttribute("message", e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "warning");
        } catch (Exception e) {
            log.error("Reject error for docId: {}, lineSqn: {}", docId, lineSqn, e);
            redirectAttributes.addFlashAttribute("message", "반려 처리 중 오류가 발생했습니다.");
            redirectAttributes.addFlashAttribute("messageType", "danger");
        }

        return "redirect:/approval/main";
    }

    @PostMapping("/saveRetractedAsTemp")
    public String saveRetractedAsTemp(
            @RequestParam String docId,
            @RequestParam String htmlData,
            RedirectAttributes redirectAttributes
    ) {
        try {
            int affectedRows = docService.saveRetractedAsTemp(docId, htmlData);
            if (affectedRows > 0) {
                redirectAttributes.addFlashAttribute("message", "회수된 문서를 임시 저장함에 저장했습니다.");
                redirectAttributes.addFlashAttribute("messageType", "success");
            } else {
                redirectAttributes.addFlashAttribute("message", "문서 임시 저장에 실패했습니다.");
                redirectAttributes.addFlashAttribute("messageType", "danger");
            }
        } catch (Exception e) {
            log.error("saveRetractedAsTemp error for docId: {}", docId, e);
            redirectAttributes.addFlashAttribute("message", "문서 임시 저장 중 오류가 발생했습니다.");
            redirectAttributes.addFlashAttribute("messageType", "danger");
        }

        return "redirect:/approval/main";
    }

    @PostMapping("/admin/delete")
    public String adminDeleteApproval(
            @RequestParam String docId,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        if (!isAdmin(authentication)) {
            redirectAttributes.addFlashAttribute("message", "관리자만 결재 문서를 삭제할 수 있습니다.");
            redirectAttributes.addFlashAttribute("messageType", "danger");
            return "redirect:/approval/main";
        }

        try {
            int affectedRows = docService.softDeleteDocumentAsAdmin(docId);
            if (affectedRows > 0) {
                redirectAttributes.addFlashAttribute("message", "결재 문서를 삭제했습니다.");
                redirectAttributes.addFlashAttribute("messageType", "success");
            } else {
                redirectAttributes.addFlashAttribute("message", "삭제할 결재 문서를 찾지 못했습니다.");
                redirectAttributes.addFlashAttribute("messageType", "warning");
            }
        } catch (Exception e) {
            log.error("adminDeleteApproval error for docId: {}", docId, e);
            redirectAttributes.addFlashAttribute("message", "결재 문서 삭제 중 오류가 발생했습니다.");
            redirectAttributes.addFlashAttribute("messageType", "danger");
        }

        return "redirect:/approval/main";
    }
}

