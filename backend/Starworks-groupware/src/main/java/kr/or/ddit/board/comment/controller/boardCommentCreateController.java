package kr.or.ddit.board.comment.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import kr.or.ddit.board.comment.service.BoardCommentService;
import kr.or.ddit.comm.validate.InsertGroupNotDefault;
import kr.or.ddit.vo.BoardCommentVO;
import lombok.RequiredArgsConstructor;

/**
 * 
 * @author 임가영
 * @since 2025. 9. 29.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 29.     	임가영           최초 생성
 *
 * </pre>
 */
@Controller
@RequiredArgsConstructor
public class boardCommentCreateController {
	
	private final BoardCommentService service;

	@PostMapping("/board/community/{pstId}/comment")
	public String createBoardComment(
		@RequestParam(required = false) String cmntSqn,
		@PathVariable String pstId, 
		@Validated(InsertGroupNotDefault.class) @ModelAttribute(name = "boardComment") BoardCommentVO boardComment,
		BindingResult errors,
		RedirectAttributes redirectAttributes,
		Authentication authentication
	) {
		if(!errors.hasErrors()) {
			// 댓글 등록
			boardComment.setCrtUserId(authentication.getName());
			boardComment.setUpCmntSqn(cmntSqn);
			boolean success = service.createBoardComment(boardComment);
			
			if(success) {
				redirectAttributes.addFlashAttribute("icon", "success");
				redirectAttributes.addFlashAttribute("toastMessage", "댓글 등록에 성공했습니다!");
			} else {
				redirectAttributes.addFlashAttribute("icon", "error");
				redirectAttributes.addFlashAttribute("toastMessage", "댓글 등록에 실패했습니다.");
			}
		} else {
			redirectAttributes.addFlashAttribute("boardComment", boardComment);
			String errorName = null;
			if(cmntSqn == null) {
				errorName = BindingResult.MODEL_KEY_PREFIX + "boardComment";
			} else {
				errorName = BindingResult.MODEL_KEY_PREFIX + "commentReply";
			}
			redirectAttributes.addFlashAttribute("icon", "error");
			redirectAttributes.addFlashAttribute("toastMessage", "댓글 등록에 실패했습니다.");
			redirectAttributes.addFlashAttribute(errorName, errors);
		}
			
		// 성공여부에 상관없이 detail 페이지로 이동
		return "redirect:/board/community/" + pstId;
	}
}
