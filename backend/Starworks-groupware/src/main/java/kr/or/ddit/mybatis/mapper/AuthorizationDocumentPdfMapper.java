package kr.or.ddit.mybatis.mapper;

import org.apache.ibatis.annotations.Mapper;

import kr.or.ddit.vo.AuthorizationDocumentPdfVO;

/**
 * 
 * @author 임가영
 * @since 2025. 10. 2.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 2.     	임가영           최초 생성
 *
 * </pre>
 */
@Mapper
public interface AuthorizationDocumentPdfMapper {
	
	/**
	 * 기안문에 해당하는 pdf 파일 상세 조회
	 * @param atrzDocId 기안문 Id
	 * @return
	 */
	public AuthorizationDocumentPdfVO selectAuthorizationDocumentPdf(String atrzDocId);
	
	/**
	 * 기안서 PDf 파일 저장
	 * @param adpVO
	 * @return 성공한 레코드 수
	 */
	public int insertAuthorizationDocumentPdf(AuthorizationDocumentPdfVO adpVO);
}
