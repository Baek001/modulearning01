package kr.or.ddit.board.community.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import kr.or.ddit.board.community.service.BoardService;
import kr.or.ddit.comm.file.service.FileMasterService;
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
@RequestMapping("/board")
@RequiredArgsConstructor
@Slf4j
public class BoardCommunityRemoveController {
	
	private final BoardService service;
	
	private final FileMasterService fileMasterService;
	
	/**
	 * 사내 자유게시판 한 건 삭제
	 * @param pstId
	 * @param model
	 * @return
	 */
	@GetMapping("/community/{pstId}/remove")
	public String modifyCommunityForm(
		@PathVariable(name = "pstId") String pstId
		, RedirectAttributes redirectAttributes
	) {
		try {
			service.removeBoard(pstId);
			redirectAttributes.addFlashAttribute("icon", "trash");
			redirectAttributes.addFlashAttribute("toastMessage", "게시글이 삭제되었습니다.");
			return "redirect:/board/community";
			
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
			redirectAttributes.addFlashAttribute("icon", "error");
			redirectAttributes.addFlashAttribute("toastMessage", "게시글 등록에 실패했습니다.");
			return "redirect:/board/community/" + pstId;
		}
	}
}
