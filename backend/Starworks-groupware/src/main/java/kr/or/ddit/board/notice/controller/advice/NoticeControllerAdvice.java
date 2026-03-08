package kr.or.ddit.board.notice.controller.advice;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;


/**
 * 
 * @author 임가영
 * @since 2025. 10. 16.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 16.     	임가영	          최초 생성
 *
 * </pre>
 */
@ControllerAdvice(basePackages = "BoardCommunityControllerAdvice.java")
public class NoticeControllerAdvice {

	@ModelAttribute("boardType")
	public String categoryCodeList() {
		// 카테고리 가져오기
		return "notice";
	}
}
