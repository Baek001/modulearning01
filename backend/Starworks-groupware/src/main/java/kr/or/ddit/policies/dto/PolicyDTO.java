package kr.or.ddit.policies.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import kr.or.ddit.comm.validate.InsertGroupNotDefault;
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
public class PolicyDTO {
	@NotBlank(groups = InsertGroupNotDefault.class)
	private String featureId;     // 기능 ID
	@NotBlank(groups = InsertGroupNotDefault.class)
    private String jbgdCd;      // 최소 직급
    private String remark;        // 권한 설명
    @NotEmpty(groups = InsertGroupNotDefault.class)
    private List<String> deptList; // 적용 부서 리스트
}
