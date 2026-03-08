package kr.or.ddit.meeting.controller.advice;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 *
 * @author 임가영
 * @since 2025. 10. 17.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 17.     	임가영           최초 생성
 *
 * </pre>
 */
@ControllerAdvice(basePackages = "kr.or.ddit.meeting")
public class MeetingControllerAdvice {

	// 사이드바 메뉴 고정
	@ModelAttribute("currentMenu")
	public String sidebarCurrentMenu() {

        return "meeting";
	}
}
