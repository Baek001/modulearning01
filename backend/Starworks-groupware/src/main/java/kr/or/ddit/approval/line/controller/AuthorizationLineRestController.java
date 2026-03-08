package kr.or.ddit.approval.line.controller;

import org.springframework.web.bind.annotation.RequestMapping;

import kr.or.ddit.approval.line.service.AuthorizationLineService;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author 윤서현
 * @since 2025. 9. 26.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 26.     	윤서현	          최초 생성
 *
 * </pre>
 */
//@RestController
@RequestMapping("/rest/approval-line")
@RequiredArgsConstructor
public class AuthorizationLineRestController {

	private final AuthorizationLineService service;


}
