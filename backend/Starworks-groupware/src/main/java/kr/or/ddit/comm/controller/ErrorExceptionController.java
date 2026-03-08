package kr.or.ddit.comm.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import kr.or.ddit.comm.Constants;
import kr.or.ddit.comm.exception.OurBusinessException;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * 에러 상태코드별 에러 페이지나 예외 처리 페이지를 동적으로 처리하는 경우 사용할 컨트롤러
 * 
 * @author SEM
 * @since 2025. 9. 24.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 24.     	SEM	          최초 생성
 *
 * </pre>
 */
@Slf4j
@Controller
public class ErrorExceptionController {
	
	/**
	 * 400 에러 등의 구체적인 상태코드별 에러 페이지
	 * @param status
	 * @return
	 */
	@RequestMapping("/error/{status}")
	public String errorStatus(@PathVariable int status) {
		log.info("상태코드 {} 에러 발생", status);
		return "error/{status}";
	}
	
	/**
	 * 상태코드 500과 별개로 예외 페이지를 처리하는 경우
	 * @param exType
	 * @param exception
	 * @return
	 */
	@RequestMapping("/error/ex/{exType}")
	public String specificException(
		@PathVariable String exType,
		@RequestAttribute(value = Constants.LASTEXCEPTION) OurBusinessException exception
	) {
		log.error("마지막 발생예외", exception);
		
		return "error/exceptionView";
	}
}
