package kr.or.ddit.approval.template.dto;

import java.util.List;

import lombok.Data;

/**
 *
 * @author 임가영
 * @since 2025. 10. 14.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 14.     	임가영           최초 생성
 *
 * </pre>
 */
@Data
public class ApprovalTemplateDTO {

	private List<String> checkedTmpl; // 체크한 목록
	private String delYn;
}
