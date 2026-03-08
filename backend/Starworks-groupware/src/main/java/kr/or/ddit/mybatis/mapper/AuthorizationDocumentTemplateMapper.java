package kr.or.ddit.mybatis.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

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
 *  2025. 10. 14.		임가영			전자결재 템플릿 양식 조회(DelYn = 'N') Mapper 추가
 *
 * </pre>
 */
@Mapper
public interface AuthorizationDocumentTemplateMapper {

	/**
	 * 전자결재 템플릿 양식 추가
	 * @param AuthDocumentTemplate
	 * @return
	 */
	public int insertAuthDocumentTemplate(AuthorizationDocumentTemplateVO authDocumentTemplate);
	/**
	 * 전자결재 템플릿 양식 조회 (All)
	 * @return
	 */
	public List<AuthorizationDocumentTemplateVO> selectAuthDocumentTemplateList();
	/**
	 * 전자결재 템플릿 양식 조회(DelYn = 'N')
	 * @return
	 */
	public List<AuthorizationDocumentTemplateVO> selectAuthDocumentTemplateListDelN();
	/**
	 * 전자결재 템플릿 양식 간단 조회(htmlData X)
	 * @return
	 */
	public List<AuthorizationDocumentTemplateVO> selectAuthDocumentTemplateSimpleList();
	/**
	 * 전자결재 템플릿 양식 상세조회
	 * @param atrzDocTmplId
	 * @return
	 */
	public AuthorizationDocumentTemplateVO selectAuthDocumentTemplate(String atrzDocTmplId);
	/**
	 * 전자결재 템플릿 양식 수정
	 * @param authorizationDocumentTemplate
	 * @return
	 */
	public int updateAuthDocumentTemplate(AuthorizationDocumentTemplateVO authorizationDocumentTemplate);
	/**
	 * 전자결재 템플릿 양식 삭제
	 * @param atrzDocTmplId
	 * @return
	 */
	public int deleteAuthDocumentTemplate(AuthorizationDocumentTemplateVO authorizationDocumentTemplate);

}
