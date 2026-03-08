package kr.or.ddit.board.notice.controller;

import org.springframework.beans.factory.annotation.Autowired;
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
public class BoardNoticeModfiyController {

	@Autowired
	private BoardService service;

	/**
	 * 공지사항 수정 페이지로 이동
	 * @return
	 */
	@GetMapping("/notice/{pstId}/edit")
	public String modifyNoticeForm(@PathVariable(name = "pstId") String pstId, Model model) {
		if(!model.containsAttribute("board")) {
			BoardVO board = service.readBoard(pstId);
			board.setBbsCtgrCd("F101");
			model.addAttribute("board", board);
		}
		return "community/community-notice-create";
	}

	@PostMapping("/notice/{pstId}/edit")
	public String modifyNotice(
		@PathVariable String pstId,
		@Validated(UpdateGroup.class) @ModelAttribute(name = "board") BoardVO board,
		BindingResult errors,
		RedirectAttributes redirectAttributes
	) {
		// Ensure fixedYn is 'N' if not explicitly set by checkbox (i.e., unchecked)
		if (board.getFixedYn() == null || board.getFixedYn().isEmpty()) {
		    board.setFixedYn("N");
		}

		if(!errors.hasErrors()) {
			// 만약 검증에 통과했으면 수정 후 detail 페이지로 이동
			service.modifyBoard(board);
			redirectAttributes.addFlashAttribute("message", "게시글 수정에 성공했습니다!");
			redirectAttributes.addFlashAttribute("icon", "success");
			return "redirect:/board/notice/" + board.getPstId();
		} else {
			// 만약 검증에 실패했으면 다시 수정 폼으로 이동
			redirectAttributes.addFlashAttribute("board", board);
			String errorName = BindingResult.MODEL_KEY_PREFIX + "board";
			redirectAttributes.addFlashAttribute(errorName, errors);
			redirectAttributes.addFlashAttribute("message", "게시글 수정에 실패했습니다.");
			redirectAttributes.addFlashAttribute("icon", "error");
			return "redirect:/board/notice/" + pstId + "/edit";
		}
	}
}
