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
 *  2025. 9. 26.     	장어진	          요소 추가 (vactExpln)
 *  2025. 9. 27.     	장어진	          LocalDate -> LocalDateTime으로 수정
 *
 * </pre>
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class VactionVO {
	
	@Include
	@NotBlank
	private Integer vactSqn;
	
	@NotBlank
	private String atrzDocId;
	
	@NotBlank
	private String vactUserId;
	
	@NotBlank
	private String vactCd;
	
	@NotBlank
	private LocalDateTime vactBgngDt;
	
	@NotBlank
	private LocalDateTime vactEndDt;
	
	@NotBlank
	private Integer useVactCnt;
	
	private String allday;
		
	private String vactExpln;
}
