package kr.or.ddit.approval.service.impl;

import org.springframework.stereotype.Service;

import kr.or.ddit.approval.service.AuthorizationDocumentPdfService;
import kr.or.ddit.mybatis.mapper.AuthorizationDocumentPdfMapper;
import kr.or.ddit.vo.AuthorizationDocumentPdfVO;
import lombok.RequiredArgsConstructor;

/**
 * 
 * @author 임가영
 * @since 2025. 10. 3.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 3.     	임가영	        최초 생성
 *
 * </pre>
 */
@Service
@RequiredArgsConstructor
public class AuthorizationDocumentPdfServiceImpl implements AuthorizationDocumentPdfService {
	
	private final AuthorizationDocumentPdfMapper mapper;
	
	/**
	 * 최종 승인 완료 문서 pdf 파일 한 건을 조회하는 서비스
	 * @param atrzDocId 문서일련번호
	 * @return
	 */
	public AuthorizationDocumentPdfVO readAuthorizationDocumentPdf(String atrzDocId) {
		return mapper.selectAuthorizationDocumentPdf(atrzDocId);
	}

}
