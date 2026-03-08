package kr.or.ddit.board.community.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import kr.or.ddit.board.community.service.BoardService;
import kr.or.ddit.comm.validate.UpdateGroup;
import kr.or.ddit.vo.BoardVO;
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
 *   수정일      			수정자           		수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 26.     	임가영	       		최초 생성
 *
 * </pre>
 */
//삭제 후 합칠 예정(임시 작성)
@Controller
@RequestMapping("/board")
@RequiredArgsConstructor
@Slf4j
public class BoardCommunityModifyController {
	
	private final BoardService service;
	
	/**
	 * 자유게시판 수정 페이지로 이동
	 * @return
	 */
	@GetMapping("/community/{pstId}/edit")
	public String modifyCommunityForm(@PathVariable(name = "pstId") String pstId, Model model) {
		if(!model.containsAttribute("board")) {
			BoardVO board = service.readBoard(pstId);
			model.addAttribute("board", board);
		}
		return "community/community-board-create";
	}
	
	@PostMapping("/community/{pstId}/edit")
	public String modifyCommunity(
		@PathVariable String pstId,
		@Validated(UpdateGroup.class) @ModelAttribute(name = "board") BoardVO board,
		BindingResult errors,
		RedirectAttributes redirectAttributes
	) {
		if(!errors.hasErrors()) {
			// 만약 검증에 통과했으면 수정 후 detail 페이지로 이동
			service.modifyBoard(board);
			redirectAttributes.addFlashAttribute("message", "게시글 수정에 성공했습니다!");
			redirectAttributes.addFlashAttribute("icon", "success");
			return "redirect:/board/community/" + board.getPstId();
		} else {
			// 만약 검증에 실패했으면 다시 수정 폼으로 이동
			redirectAttributes.addFlashAttribute("board", board);
			String errorName = BindingResult.MODEL_KEY_PREFIX + "board";
			redirectAttributes.addFlashAttribute(errorName, errors);
			redirectAttributes.addFlashAttribute("message", "게시글 수정에 실패했습니다.");
			redirectAttributes.addFlashAttribute("icon", "error");
			return "redirect:/board/community/" + pstId + "/edit";
		}		
	}

}
