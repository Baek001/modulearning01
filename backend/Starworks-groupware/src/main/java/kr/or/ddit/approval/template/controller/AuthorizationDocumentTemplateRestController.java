package kr.or.ddit.approval.template.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import kr.or.ddit.approval.template.dto.ApprovalTemplateDTO;
import kr.or.ddit.approval.template.service.AuthorizationDocumentTemplateService;
import kr.or.ddit.comm.validate.InsertGroupNotDefault;
import kr.or.ddit.comm.validate.UpdateGroup;
import kr.or.ddit.comm.validate.UpdateGroupNotDefault;
import kr.or.ddit.vo.AuthorizationDocumentTemplateVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author 윤서현
 * @since 2025. 9. 26.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           		수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 26.     	윤서현	          	최초 생성
 *  2025. 10. 14.		임가영				delYn 에 따라 조회 가능한 템플릿 양식 변경
 * </pre>
 */
@RestController
@RequestMapping("/rest/approval-template")
@RequiredArgsConstructor
@Slf4j
public class AuthorizationDocumentTemplateRestController {

	private final AuthorizationDocumentTemplateService service;

	/**
	 * 전자결재 템플릿 양식 조회. RestController
	 * @return
	 */
	@GetMapping
	public List<AuthorizationDocumentTemplateVO> readAuthDocumentTemplateList(@RequestParam(defaultValue = "all") String filter){
		return service.readAuthDocumentTemplateList(filter);
	}

	/**
	 * 전자결재 템플릿 양식 상세조회. RestController
	 * @param atrzDocTmplId
	 * @return
	 */
	@GetMapping("/{atrzDocTmplId}")
	public AuthorizationDocumentTemplateVO readAuthDocumentTemplate(@PathVariable String atrzDocTmplId) {
		return service.readAuthDocumentTemplate(atrzDocTmplId);
	}


	@PostMapping
	public Map<String, Object> createAuthorizationDocumentTemplate(
		@RequestBody @Validated(InsertGroupNotDefault.class) AuthorizationDocumentTemplateVO template,
		BindingResult errors
	) {

		Map<String, Object> respMap = new HashMap<>();

		boolean success = false;

		if (!errors.hasErrors()) {
			success = service.createAuthDocumentTemplate(template);
		} else {
			respMap.put("message", "필수 값을 입력하세요.");
		}

		respMap.put("success", success);
		return respMap;
	}

	@PutMapping
	public Map<String, Object> modifyAuthorizationDocumentTemplate(
			@RequestBody @Validated(UpdateGroupNotDefault.class) AuthorizationDocumentTemplateVO template,
			BindingResult errors
		) {

			Map<String, Object> respMap = new HashMap<>();

			boolean success = false;

			if (!errors.hasErrors()) {
				success = service.modifyAuthDocumentTemplate(template);
			} else {
				respMap.put("message", "필수 값을 입력하세요.");
			}

			respMap.put("success", success);
			return respMap;
		}

	@DeleteMapping
	public Map<String, Object> removeAuthorizationDocumentTemplate(@RequestBody ApprovalTemplateDTO approvalTemplateDTO) {

		boolean success = false;
		success = service.removeAuthDocumentTemplate(approvalTemplateDTO);

		return Map.of("success", success);
	}
}
