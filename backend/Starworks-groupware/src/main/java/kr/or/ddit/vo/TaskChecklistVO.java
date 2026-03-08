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
public class TaskChecklistVO {
	
	@Include
	private Integer chklistSqn; //체크리스트 sqn
	
	private String taskId; //업무 ID
	
	@NotBlank
	private String taskPicId; //업무 담당자 ID
	private String chklistCont; //체크리스트 목표
	private String compltYn; //체크리스트 달성 여부
}
