package kr.or.ddit.approval.document.controller.advice;

import java.util.List;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import kr.or.ddit.approval.template.service.AuthorizationDocumentTemplateService;
import kr.or.ddit.mybatis.mapper.AuthorizationDocumentTemplateMapper;
import kr.or.ddit.vo.AuthorizationDocumentTemplateVO;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author 임가영
 * @since 2025. 10. 1.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 1.     	임가영           최초 생성
 *
 * </pre>
 */
@ControllerAdvice(basePackages = "kr.or.ddit.approval.document.controller")
@RequiredArgsConstructor
public class AuthorizationDocumentAdvice {
	private final AuthorizationDocumentTemplateMapper mapper;

	@ModelAttribute("approvalTemplateList")
	public List<AuthorizationDocumentTemplateVO> approvalTemplateList() {
		// 템플릿 목록 가져오기
		return mapper.selectAuthDocumentTemplateSimpleList();
	}
	
}
