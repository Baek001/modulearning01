package kr.or.ddit.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 
 * @author 임가영
 * @since 2025. 9. 25.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 25.     	임가영	       최초 생성
 *
 * </pre>
 */
@Data
@EqualsAndHashCode
public class PoliciesDetailVO {
	
	@NotBlank
	private String featureId;
	@NotBlank
	private String jbgdCd;
	
	private String deptId;
	
	// join 필드
	private String remark; // 설명
	private String featureName; // 기능명
	
	// resultMap mapping 필드
	private String jbgdNm;
	private String deptNm;

}
