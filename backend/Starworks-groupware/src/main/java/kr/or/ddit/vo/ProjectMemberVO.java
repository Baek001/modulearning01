package kr.or.ddit.vo;

import jakarta.validation.constraints.NotBlank;
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
 *  2025. 9. 30.     	장어진	          biz_nm 추가
 *  2025. 9. 30. 		김주민			  bizUserNm, bizAuthNm 추가
 *  2025. 10. 16.		김주민			  bizUserJobNm 추가
 *
 * </pre>
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ProjectMemberVO {
	@NotBlank
	@Include
	private String bizId;

	@Include
	@NotBlank
	private String bizUserId;
	private String bizAuthCd;

	private String bizUserNm;
	private String bizAuthNm;
	private String bizUserDeptNm; // 참여자 부서명
	private String bizUserJobNm; // 참여자 직급명

	//프로젝트 이름 들고오기 위한 추가 요소
	private String bizNm;

	private String filePath;
}
