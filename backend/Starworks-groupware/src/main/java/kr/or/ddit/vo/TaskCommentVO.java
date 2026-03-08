package kr.or.ddit.vo;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import kr.or.ddit.comm.validate.InsertGroup;
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
 *  2025. 10. 09. 		김주민			  users 추가
 *
 * </pre>
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TaskCommentVO {
	
	@Include
	@NotBlank
	private Integer taskCommSqn;
	
	@NotBlank(groups = InsertGroup.class)
	private String taskId;
	
	private String contents;
	
	@NotBlank(groups = InsertGroup.class)
	private String crtUserId;
	private LocalDateTime crtDt;
	
	private UsersVO users;
}
