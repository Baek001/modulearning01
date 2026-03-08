package kr.or.ddit.calendar.controller.advice;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 
 * @author 임가영
 * @since 2025. 10. 15.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 15.     	임가영	          최초 생성
 *
 * </pre>
 */
@ControllerAdvice(basePackages = "kr.or.ddit.calendar")
public class CalendarControllerAdvice {

	// 사이드바 메뉴 고정
	@ModelAttribute("currentMenu")
	public String sidebarCurrentMenu() {
		
        return "calendar";
	}
}
