package kr.or.ddit.vo;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import kr.or.ddit.comm.paging.SimpleSearch;
import kr.or.ddit.comm.validate.InsertGroup;
import kr.or.ddit.comm.validate.UpdateGroup;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 *
 * @author 윤서현
 * @since 2025. 10. 17.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 17.     	윤서현	          최초 생성
 *
 * </pre>
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class MeetingMemoVO {

	@NotNull(groups = UpdateGroup.class)
	private Integer memoId;

	@NotBlank(groups = InsertGroup.class)
	private String userId;
	private String title;
	private String contents;
	private LocalDate crtDt;

	// 검색을 위한..
	SimpleSearch simpleSearch;
}
