package kr.or.ddit.board.notice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import kr.or.ddit.board.community.service.BoardService;
import kr.or.ddit.comm.validate.InsertGroup;
import kr.or.ddit.security.CustomUserDetails;
import kr.or.ddit.vo.BoardVO;
import kr.or.ddit.vo.UsersVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author 홍현택
 * @since 2025. 9. 27.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 27.     	홍현택	          최초 생성
 *
 * </pre>
 */
@Controller
@RequestMapping("/board")
@RequiredArgsConstructor
@Slf4j
public class BoardNoticeCreateController {

	@Autowired
	private BoardService service;
	
	/**
	 * 공지사항 작성 페이지로 이동
	 * @return
	 */
	@GetMapping("/notice/create")
	public String createCommunityNoticeFrom(Model model) {
		if(!model.containsAttribute("board")) {
			BoardVO board = new BoardVO();
			board.setBbsCtgrCd("F101");
			board.setFixedYn("N"); // 공지사항 고정 여부 기본값 'N'으로 설정
			model.addAttribute("board", board);
		}
		return "community/community-notice-create";
	}
	
	/**
	 * 공지사항 작성글 등록
	 * 
	 * @param model
	 * @return
	 */
	@PostMapping("/notice/create")
	public String createCommunity(
			@Validated(InsertGroup.class) @ModelAttribute("board") BoardVO board,
			BindingResult errors, 
			RedirectAttributes redirectAttributes, 
			Authentication authentication

	) {
		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
		UsersVO realUser = userDetails.getRealUser();

		board.setCrtUserId(realUser.getUserId());

		// Ensure fixedYn is 'N' if not explicitly set by checkbox (i.e., unchecked)
		if (board.getFixedYn() == null || board.getFixedYn().isEmpty()) {
		    board.setFixedYn("N");
		}

		if (!errors.hasErrors()) {
			service.createBoard(board);
			redirectAttributes.addFlashAttribute("icon", "success");
			redirectAttributes.addFlashAttribute("toastMessage", "게시글 등록에 성공했습니다!");
			return "redirect:/board/notice";
		} else {
			// 검증 실패
			redirectAttributes.addFlashAttribute("board", board);
			String errorName = BindingResult.MODEL_KEY_PREFIX + "board";
			redirectAttributes.addFlashAttribute(errorName, errors);
			redirectAttributes.addFlashAttribute("toastMessage", "게시글 등록에 실패했습니다.");
			redirectAttributes.addFlashAttribute("icon", "error");
			// 글 작성 페이지로 이동
			return "redirect:/board/notice/create";
		}
	}
}
