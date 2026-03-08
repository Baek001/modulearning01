package kr.or.ddit.approval.template.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import kr.or.ddit.approval.template.dto.ApprovalTemplateDTO;
import kr.or.ddit.comm.exception.EntityNotFoundException;
import kr.or.ddit.mybatis.mapper.AuthorizationDocumentTemplateMapper;
import kr.or.ddit.vo.AuthorizationDocumentTemplateVO;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

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
@Service
@RequiredArgsConstructor
public class AuthorizationDocumentTemplateServiceImpl implements AuthorizationDocumentTemplateService{

	private final AuthorizationDocumentTemplateMapper mapper;

	/**
	 * 결재양식 등록
	 */
	@Override
	public boolean createAuthDocumentTemplate(AuthorizationDocumentTemplateVO authDocumentTemplate) {
		return mapper.insertAuthDocumentTemplate(authDocumentTemplate) > 0;
	}

	/**
	 * 결재양식 리스트 조회
	 */
	@Override
	public List<AuthorizationDocumentTemplateVO> readAuthDocumentTemplateList(String filter) {
		if ("user".equals(filter)) {
			return mapper.selectAuthDocumentTemplateListDelN();
		}

		return mapper.selectAuthDocumentTemplateList();
	}

	/**
	 * 결재양식 한건 조회
	 */
	@Override
	public AuthorizationDocumentTemplateVO readAuthDocumentTemplate(String atrzDocTmplId) {
		AuthorizationDocumentTemplateVO authDocumentTemplate = mapper.selectAuthDocumentTemplate(atrzDocTmplId);
		if(authDocumentTemplate == null) {
			throw new EntityNotFoundException(authDocumentTemplate);
		}
		return authDocumentTemplate;
	}

	@Override
	public boolean modifyAuthDocumentTemplate(AuthorizationDocumentTemplateVO authDocumentTemplate) {
		return mapper.updateAuthDocumentTemplate(authDocumentTemplate) > 0;
	}

	/**
	 * 결재양식 삭제
	 */
	@Override
	public boolean removeAuthDocumentTemplate(ApprovalTemplateDTO templateDTO) {
		boolean success = false;

		List<String> atrzDocTmplIdList = templateDTO.getCheckedTmpl();
		String delYn = templateDTO.getDelYn();

		AuthorizationDocumentTemplateVO adtVO = new AuthorizationDocumentTemplateVO();

		int rowcnt = 0;
		for (String atrzDocTmplId : atrzDocTmplIdList) {
			adtVO.setAtrzDocTmplId(atrzDocTmplId);
			adtVO.setDelYn(delYn);

			rowcnt += mapper.deleteAuthDocumentTemplate(adtVO);
		}

		if (rowcnt == atrzDocTmplIdList.size()) { success = true; }

		return success;
	}

}
