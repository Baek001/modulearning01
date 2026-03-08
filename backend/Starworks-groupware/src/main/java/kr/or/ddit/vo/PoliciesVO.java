package kr.or.ddit.vo;

import java.time.LocalDate;
import java.util.List;

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
 *  2025. 9. 25			임가영			직급, 부서 코드 삭제
 *
 * </pre>
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PoliciesVO {

	@Include
	@NotBlank
	private String featureId;
	private String remark; // 설명
	private LocalDate crtDt; // 권한 생성일
	private LocalDate modDt; // 최종 수정일

	// join 필드 추가
	private String featureName;
	// detail mapping 정보
	private List<PoliciesDetailVO> policiesDetailList;
}
