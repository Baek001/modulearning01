package kr.or.ddit.board.notice.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import kr.or.ddit.board.community.service.BoardService;
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
 *  2025. 9. 27.     	홍현택	        최초 생성
 *  2025.10.  1.		임가영			보완 주석 추가
 *  2025.10.  2.		홍현택			주석 추가
 * </pre>
 */
@Controller
@RequestMapping("/board")
@RequiredArgsConstructor
@Slf4j
public class BoardNoticeRemoveController {
	
	
	private final BoardService service;
	
	/**
	 *  공지사항 삭제
	 * @param pstId
	 * @param model
	 * @return
	 */
	@GetMapping("/notice/{pstId}/remove")
	public String modifyNoticeForm(
		@PathVariable(name = "pstId") String pstId
		, RedirectAttributes redirectAttributes
	) {
		try {
			service.removeBoard(pstId);
			redirectAttributes.addFlashAttribute("icon", "trash");
			redirectAttributes.addFlashAttribute("toastMessage", "게시글이 삭제되었습니다.");
			return "redirect:/board/notice";
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
			redirectAttributes.addFlashAttribute("icon", "error");
			redirectAttributes.addFlashAttribute("toastMessage", "게시글 등록에 실패했습니다.");
			return "redirect:/board/notice/" + pstId;
		}
	}
}
