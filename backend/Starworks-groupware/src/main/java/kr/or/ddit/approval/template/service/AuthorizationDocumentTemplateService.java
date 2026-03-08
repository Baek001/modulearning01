package kr.or.ddit.approval.template.service;

import java.util.List;

import kr.or.ddit.approval.template.dto.ApprovalTemplateDTO;
import kr.or.ddit.vo.AuthorizationDocumentTemplateVO;

/**
 *
 * @author 윤서현
 * @since 2025. 9. 25.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 25.     	윤서현	          최초 생성
 *
 * </pre>
 */
public interface AuthorizationDocumentTemplateService {
	/**
	 * 전자결재 템플릿 양식 추가
	 * @param authDocumentTemplate
	 * @return
	 */
	public boolean createAuthDocumentTemplate(AuthorizationDocumentTemplateVO authDocumentTemplate);
	/**
	 * 전자결재 템플릿 양식 조회
	 * @return role 관리자, 사용자 구분
	 */
	public List<AuthorizationDocumentTemplateVO> readAuthDocumentTemplateList(String role);
	/**
	 * 전자결재 템플릿 양식 상세조회
	 * @param atrzDocTmplId
	 * @return
	 */
	public AuthorizationDocumentTemplateVO readAuthDocumentTemplate(String atrzDocTmplId);
	/**
	 * 전자결재 템플릿 수정
	 * @param authorizationDocumentTemplate
	 * @return
	 */
	public boolean modifyAuthDocumentTemplate(AuthorizationDocumentTemplateVO authorizationDocumentTemplate);
	/**
	 * 전자결재 템플릿 양식 삭제
	 * @param approvalTemplateDTO
	 * @return
	 */
	public boolean removeAuthDocumentTemplate(ApprovalTemplateDTO approvalTemplateDTO);
}
