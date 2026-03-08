package kr.or.ddit.board.community.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
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
 * @author 임가영
 * @since 2025. 9. 26.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 26.     	임가영	       최초 생성
 *
 * </pre>
 */
//삭제 후 합칠 예정(임시 작성)
@Controller
@RequiredArgsConstructor
@Slf4j
public class BoardCommunityCreateController {
	
	private final BoardService service;
	
	/**
	 * 자유게시판 작성 페이지로 이동
	 * @return
	 */
	@GetMapping("/board/community/create")
	public String createCommunityForm() {
		return "community/community-board-create";
	}
	
	/**
	 * 자유게시판 글 등록
	 * @param model
	 * @return
	 */
	@PostMapping("/board/community/create")
	public String createCommunity (
		@Validated(InsertGroup.class) @ModelAttribute("board") BoardVO board,
		BindingResult errors,
		RedirectAttributes redirectAttributes,
		Authentication authentication
		
	) {
		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
		UsersVO realUser = userDetails.getRealUser();
		
		board.setCrtUserId(realUser.getUserId());
		if(!errors.hasErrors()) {
			// 검증 성공 후 insert
			service.createBoard(board);
			redirectAttributes.addFlashAttribute("icon", "success");
			redirectAttributes.addFlashAttribute("toastMessage", "게시글 등록에 성공했습니다!");
			return "redirect:/board/community";
		} else {
			// 검증 실패
			redirectAttributes.addFlashAttribute("board", board);
			String errorName = BindingResult.MODEL_KEY_PREFIX + "board";
			redirectAttributes.addFlashAttribute(errorName, errors);
			redirectAttributes.addFlashAttribute("toastMessage", "게시글 등록에 실패했습니다.");
			redirectAttributes.addFlashAttribute("icon", "error");
			// 글 작성 페이지로 이동
			return "redirect:/board/community/create";
		}
	}
}
