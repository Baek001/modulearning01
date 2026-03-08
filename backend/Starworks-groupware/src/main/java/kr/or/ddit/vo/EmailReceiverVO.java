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
 *  2025.10. 13.		홍현택			수신자 이름을 가져오기 위해 userName 추가
 *
 * </pre>
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class EmailReceiverVO {

	@Include
	@NotBlank
	private Integer mailRcvrSqn;

	@NotBlank
	private String emailContId;

	@NotBlank
	private String userId;

	// USERS 테이블과 조인해서 가져올 수신자 이름
	private String userName;
}
