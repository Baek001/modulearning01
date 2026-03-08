package kr.or.ddit.mypage.controller;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import kr.or.ddit.approval.otp.service.OtpService;
import kr.or.ddit.comm.file.service.FileDetailService;
import kr.or.ddit.mypage.service.MyPageService;
import kr.or.ddit.security.CustomUserDetails;
import kr.or.ddit.users.service.UsersService;
import kr.or.ddit.vo.FileDetailVO;
import kr.or.ddit.vo.UsersVO;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author 윤서현
 * @since 2025. 10. 14.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           	  수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 14.     	윤서현	          최초 생성
 *  2025. 10. 15.		임가영			Authentication 갱신하는 코드 추가
 *  2025. 10. 21.		홍현택			OTP 활성화 여부 확인 로직 추가
 * </pre>
 */
@Controller
@RequiredArgsConstructor
public class MyPageReadController {

	private final MyPageService mypageService;
	private final UsersService service;
	private final FileDetailService fileDetailService;
    private final OtpService otpService; // OTP 서비스 주입

	@GetMapping("/mypage")
	public String myPage(
		@AuthenticationPrincipal CustomUserDetails userDetail, Model model
	) {

		UsersVO user = service.readUser(userDetail.getUsername());
		List<FileDetailVO> fileList = fileDetailService.readFileDetailList(user.getUserImgFileId());

        // OTP 활성화 여부 확인
        String secret = otpService.getUserOtpSecret(userDetail.getUsername());
        model.addAttribute("isOtpEnabled", StringUtils.isNotBlank(secret));

		model.addAttribute("userInfo", user);
		model.addAttribute("fileList", fileList);

		return "login/mypage";
	}

	@PostMapping("/mypage/update")
	public String modifyMyPage(
		@AuthenticationPrincipal CustomUserDetails userDetail,
		@RequestParam(value = "fileList", required = false) MultipartFile fileList,
		@ModelAttribute UsersVO updateUser,
		RedirectAttributes redirectAttributes
	) {
		UsersVO loginUser = userDetail.getRealUser();
		mypageService.updateUserInfo(loginUser.getUserId(), updateUser, fileList);

		// 세션 정보 갱신
		// 1. 현재 SecurityContext에서 사용자의 인증 정보(Authentication)를 가져오기
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		// 2. DB에서 방금 수정된 최신 사용자 정보를 다시 조회.
		UsersVO updatedUser = service.readUser(loginUser.getUserId());
		// 3. 최신 사용자 정보(updatedUser)를 기반으로 새로운 CustomUserDetails 객체(인증 주체)를 생성
		CustomUserDetails newPrincipal = new CustomUserDetails(updatedUser);
		 // 4. 새로운 인증 주체와 기존 인증 정보를 사용하여 새로운 Authentication 객체를 생성
		//     newPrincipal: 방금 생성한 새로운 인증 주체
		//     authentication.getCredentials(): 기존의 인증 정보(비밀번호 등)
		//     newPrincipal.getAuthorities(): 새로운 인증 주체의 권한정보
		Authentication newAuth = new UsernamePasswordAuthenticationToken(newPrincipal, authentication.getCredentials(), newPrincipal.getAuthorities());
		// 5. SecurityContext에 새로 생성한 Authentication 객체를 설정하여 세션 정보를 갱신
		SecurityContextHolder.getContext().setAuthentication(newAuth);

		redirectAttributes.addFlashAttribute("icon", "success");
		redirectAttributes.addFlashAttribute("toastMessage", "수정완료되었습니다.");

		return "redirect:/mypage";
	}
}