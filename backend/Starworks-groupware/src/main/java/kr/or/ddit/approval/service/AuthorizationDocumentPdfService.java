package kr.or.ddit.approval.service;

import kr.or.ddit.vo.AuthorizationDocumentPdfVO;

/**
 * 최종 승인 완료 문서 pdf 파일 서비스
 * @author 임가영
 * @since 2025. 10. 3.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 3.     	임가영	       최초 생성
 *
 * </pre>
 */
public interface AuthorizationDocumentPdfService {

	/**
	 * 최종 승인 완료 문서 pdf 파일 한 건을 조회하는 서비스
	 * @param atrzDocId 문서일련번호
	 * @return
	 */
	public AuthorizationDocumentPdfVO readAuthorizationDocumentPdf(String atrzDocId);
}
