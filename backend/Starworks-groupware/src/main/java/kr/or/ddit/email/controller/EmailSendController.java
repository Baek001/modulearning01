package kr.or.ddit.email.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import kr.or.ddit.email.content.service.EmailContentService; // EmailContentService 주입
import kr.or.ddit.email.service.EmailSendService;
import kr.or.ddit.security.CustomUserDetails;
import kr.or.ddit.vo.EmailContentVO;
import kr.or.ddit.vo.UsersVO;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author 홍현택
 * @since 2025. 10. 10.
 * @see
 *
 *      <pre>
* << 개정이력(Modification Information) >>
*
*   수정일      			수정자           수정내용
*  -----------   	-------------    ---------------------------
*  2025. 10. 10.     	홍현택	          최초 생성
*  2025. 10. 13.     	홍현택	          메일 발송 기능 구현
*  2025. 10. 13.		홍현택			임시저장 메일  처리
*  2025. 10. 14.		홍현택			답장, 전달 기능 구현
 *
 *      </pre>
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/mail")
public class EmailSendController {

	private final EmailSendService emailSendService;
	private final EmailContentService emailContentService; // EmailContentService 주입

	/**
	 * 이메일 작성 페이지로 이동
	 *
	 * @return
	 */
	@GetMapping("/send")
	public String emailSendForm(
			@RequestParam(required = false) String draftId,
			Authentication authentication,
			Model model
			) {
		// draftId가 있으면 임시저장된 메일 내용을 불러와 모델에 추가
		if (draftId != null && !draftId.isEmpty()) {
			CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
			String userId = userDetails.getRealUser().getUserId();
			EmailContentVO draftEmail = emailContentService.readDraft(draftId, userId);
			model.addAttribute("draftEmail", draftEmail);
		}
		return "mail/mail-send";
	}

	/**
	 * 답장 이메일 작성 페이지로 이동 (원본 메일 정보 포함)
	 * @param originalEmailContId 원본 메일 ID
	 * @param authentication
	 * @param model
	 * @return
	 */
	@GetMapping("/reply")
	public String emailReplyForm(
			@RequestParam String originalEmailContId,
			Authentication authentication,
			Model model
			) {
		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
		String userId = userDetails.getRealUser().getUserId();
		EmailContentVO originalEmail = emailContentService.readEmailDetail(originalEmailContId, userId);
		model.addAttribute("originalEmail", originalEmail);
		return "mail/mail-reply"; // 답장 작성 페이지 뷰 이름
	}

	/**
	 * 이메일 발송 처리
	 *
	 * @param emailContentVO
	 * @param user
	 * @return
	 */
	@PostMapping("/send")
	public String emailSend(
			@ModelAttribute EmailContentVO emailContentVO,
			Authentication authentication
		) {
		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
		UsersVO user = userDetails.getRealUser();
		emailContentVO.setUserId(user.getUserId());

		// 중요 메일 여부 처리
		if ("Y".equals(emailContentVO.getImptMailYn())) {
			emailContentVO.setImptMailYn("Y");
		} else {
			emailContentVO.setImptMailYn("N");
		}

		emailSendService.sendEmail(emailContentVO);
		return "redirect:/mail/list?mailboxTypeCd=G102"; // 보낸 편지함으로 리다이렉트
	}



	/**
	 * 이메일 임시저장 처리
	 *
	 * @param emailContentVO
	 * @param authentication
	 * @return
	 */
	@PostMapping("/saveDraft")
	public String emailSaveDraft(
			@ModelAttribute EmailContentVO emailContentVO,
			@RequestParam(required = false) String[] recipients, // 수신자 정보 추가
			Authentication authentication
		) {
		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
		UsersVO user = userDetails.getRealUser();
		emailContentVO.setUserId(user.getUserId());

		// 중요 메일 여부 처리
		if (emailContentVO.getImptMailYn() == null || emailContentVO.getImptMailYn().isEmpty()) {
			emailContentVO.setImptMailYn("N");
		}

		emailSendService.saveDraft(emailContentVO, recipients); // 서비스에 수신자 정보 전달
		return "redirect:/mail/list?mailboxTypeCd=G103"; // 임시 보관함으로 리다이렉트
	}

	/**
	 * 전달 이메일 작성 페이지로 이동 (원본 메일 정보 포함)
	 * @param originalEmailContId 원본 메일 ID
	 * @param authentication
	 * @param model
	 * @return
	 */
	@GetMapping("/forward")
	public String emailForwardForm(
			@RequestParam String originalEmailContId,
			Authentication authentication,
			Model model
			) {
		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
		String userId = userDetails.getRealUser().getUserId();
		EmailContentVO originalEmail = emailContentService.readEmailDetail(originalEmailContId, userId);
		model.addAttribute("originalEmail", originalEmail);
		return "mail/mail-forward"; // 전달 작성 페이지 뷰 이름
	}

	/**
	 * 이메일 답장
	 * @param originalEmailContId
	 * @param emailContentVO
	 * @param recipients
	 * @param authentication
	 * @return
	 */
	@PostMapping("/reply")
	public String emailReply(
			@RequestParam String originalEmailContId,
			@ModelAttribute EmailContentVO emailContentVO,
			@RequestParam String[] recipients,
			Authentication authentication
			) {
		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
		String userId = userDetails.getRealUser().getUserId();
		emailContentVO.setUserId(userId);

		// 중요 메일 여부 처리
		if (emailContentVO.getImptMailYn() == null || emailContentVO.getImptMailYn().isEmpty()) {
			emailContentVO.setImptMailYn("N");
		}

		emailSendService.replyEmail(originalEmailContId, emailContentVO, recipients);
		return "redirect:/mail/list?mailboxTypeCd=G102"; // 보낸 편지함으로 리다이렉트
	}

	/**
	 * 전달 이메일 발송 처리
	 * @param emailContentVO 전달할 메일 내용 (원본 메일 ID 포함)
	 * @param recipients 수신자 목록
	 * @param authentication
	 * @return
	 */
	@PostMapping("/forward")
	public String emailForward(
			@RequestParam String originalEmailContId,
			@ModelAttribute EmailContentVO emailContentVO,
			@RequestParam String[] recipients,
			Authentication authentication
			) {
		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
		String userId = userDetails.getRealUser().getUserId();
		emailContentVO.setUserId(userId);

		// 중요 메일 여부 처리
		if (emailContentVO.getImptMailYn() == null || emailContentVO.getImptMailYn().isEmpty()) {
			emailContentVO.setImptMailYn("N");
		}

		emailSendService.forwardEmail(originalEmailContId, emailContentVO, recipients);
		return "redirect:/mail/list?mailboxTypeCd=G102"; // 보낸 편지함으로 리다이렉트
	}
}
