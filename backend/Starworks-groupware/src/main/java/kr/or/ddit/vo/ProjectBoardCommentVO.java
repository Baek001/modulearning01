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
 *  2025.10. 06.		장어진			  users 필드 추가
 *
 * </pre>
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ProjectBoardCommentVO {
	
	@Include
	@NotBlank
	private String bizCmntId;
	
	@NotBlank
	private String bizPstId;
	
	private String contents;
	private String upBizCmntId;
	private String delYn;
	
	@NotBlank
	private String crtUserId;
	private LocalDateTime frstCrtDt;
	private String lastChgUserId;
	private LocalDateTime lastChgDt;
	
	private UsersVO users;	
}
