package kr.or.ddit.vo;

import java.time.LocalDateTime;

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
 *  2025. 9. 27.     	장어진	          LocalDate -> LocalDateTime으로 수정
 *  2025. 9. 29.     	장어진	          delYn 추가
 *
 * </pre>
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DepartmentScheduleVO {

	@Include
	@NotBlank
	private String deptSchdId;
	
	@NotBlank
	private String deptId;
	private String schdTtl;
	
	@NotBlank
	private String deptSchdCrtUserId;
	private LocalDateTime schdStrtDt;
	private LocalDateTime schdEndDt;
	private String deptSchdExpln;
	private String allday;
	
	private String delYn;
}
