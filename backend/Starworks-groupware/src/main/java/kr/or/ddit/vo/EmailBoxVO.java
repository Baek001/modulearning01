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
 *
 * </pre>
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class EmailBoxVO {
	
	@Include
	@NotBlank
	private String mailboxId;
	private String upMailboxId;
	
	@NotBlank
	private String userId;
	
	@NotBlank
	private String mailboxTypeCd;
	private String mailboxNm;
	private Integer sortNum;
	private String delYn;
}
