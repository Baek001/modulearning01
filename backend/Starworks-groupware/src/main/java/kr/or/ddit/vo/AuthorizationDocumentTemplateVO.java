package kr.or.ddit.vo;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import kr.or.ddit.comm.validate.InsertGroupNotDefault;
import kr.or.ddit.comm.validate.UpdateGroup;
import kr.or.ddit.comm.validate.UpdateGroupNotDefault;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.Include;

/**
 *
 * @author 장어진
 * @since 2025. 9. 24.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 24.     	장어진	          최초 생성
 *  2025. 10. 14.		임가영			atrzDocCd @NotBlack 주석
 * </pre>
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AuthorizationDocumentTemplateVO {

	@Include
	@NotBlank(groups = UpdateGroupNotDefault.class)
	private String atrzDocTmplId;

//	@NotBlank
	private String atrzDocCd;

	@NotBlank(groups = {InsertGroupNotDefault.class, UpdateGroupNotDefault.class})
	private String atrzDocTmplNm;

	@NotBlank(groups = InsertGroupNotDefault.class)
	private String htmlContents;
	private String apprAtrzDocYn;
	private Integer atrzSecureLvl;
	private Integer atrzSaveYear;

	private String atrzCategory; //템플릿 카테고리
	private String atrzDescription; //기안문 상세설명

	private LocalDateTime crtDt;
	private String delYn;

}
