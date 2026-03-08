package kr.or.ddit.approval.otp.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import kr.or.ddit.approval.otp.service.OtpService;
import kr.or.ddit.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;

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
 *  2025. 10. 21.     	홍현택	          최초 생성
 *
 * </pre>
 */
@Controller
@RequestMapping("/mypage/otp")
@RequiredArgsConstructor
public class OtpController {

    private final OtpService otpService;

    /**
     * OTP 설정 페이지를 보여줍니다.
     * 새로운 비밀 키와 QR 코드를 생성하여 모델에 추가합니다.
     */
    @GetMapping("/setup")
    public String showOtpSetupPage(Authentication authentication, Model model) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String userId = userDetails.getUsername();
        // 로그인 사용자의 이메일로 사용
        String userEmail = userDetails.getRealUser().getUserEmail();

        // OTP 비밀키 생성
        String secretKey = otpService.generateSecretKey();
        // 생성된 비밀키와 사용자 이메일을 바탕으로 앱에서 스캔 가능한 otpauth URL(QR 코드 데이터)을 생성. 발급자(issuer)는 '모두의 러닝'으로 설정
        String qrCodeDataUrl = otpService.getOtpQrCodeDataUrl(secretKey, userEmail, "\uBAA8\uB450\uC758 \uB7EC\uB2DD");

        model.addAttribute("secretKey", secretKey);
        model.addAttribute("qrCodeDataUrl", qrCodeDataUrl);

        return "login/otp-setup";
    }

    /**
     * 사용자가 입력한 OTP 코드를 검증하고 비밀 키를 저장합니다.
     */
    @PostMapping("/verify")
    public String verifyAndSaveOtp(
    		// 사용자가 QR 등록 단계에서 받은 비밀키
            @RequestParam String secretKey,
            // 사용자가 OTP 앱에서 확인한 6자리 숫자 코드 입력값
            @RequestParam int otpCode,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        String userId = authentication.getName();

        // 입력된 코드가 비밀키로 생성된 TOTP 값과 일치하는지 검증
        boolean isValid = otpService.validateOtp(secretKey, otpCode);

        // 성공 시
        if (isValid) {
        	//saveUserOtpSecret 로 DB에 비밀키 저장
            otpService.saveUserOtpSecret(userId, secretKey);
            redirectAttributes.addFlashAttribute("message", "OTP 설정이 완료되었습니다.");
            return "redirect:/mypage";
        // 실패 시
        } else {
        	// OTP 검증 실패 시, 새로운 비밀 키와 QR 코드를 생성하여 다시 설정하도록 유도              │
        	CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        	String userEmail = userDetails.getRealUser().getUserEmail();

        	String newSecretKey = otpService.generateSecretKey();
        	String newQrCodeDataUrl = otpService.getOtpQrCodeDataUrl(newSecretKey, userEmail, "\uBAA8\uB450\uC758 \uB7EC\uB2DD");

        	redirectAttributes.addFlashAttribute("error", "OTP 코드가 유효하지 않습니다. 새로운 QR 코드를 스캔하여 다시 시도해주세요.");
    	    redirectAttributes.addFlashAttribute("secretKey", newSecretKey);

    	    redirectAttributes.addFlashAttribute("qrCodeDataUrl", newQrCodeDataUrl);
    	    return "redirect:/mypage/otp/setup";
        }
   }

    /**
     * 사용자의 OTP 설정을 비활성화합니다.
     */
    @PostMapping("/disable")
    public String disableOtp(
    		Authentication authentication,
    		RedirectAttributes redirectAttributes
    	) {
        String userId = authentication.getName();
        // OTP 비활성화를 위해 DB의 USER_OTP_SECRET 컬럼 값을 null로 업데이트
        otpService.saveUserOtpSecret(userId, null);
        redirectAttributes.addFlashAttribute("message", "OTP가 비활성화되었습니다. 사용하시는 Authenticator 앱에서 계정을 삭제해주세요.");
        return "redirect:/mypage";
    }
}
