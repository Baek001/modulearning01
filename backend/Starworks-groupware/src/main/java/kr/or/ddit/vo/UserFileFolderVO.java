package kr.or.ddit.vo;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import kr.or.ddit.comm.validate.InsertGroupNotDefault;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.Include;

/**
 *
 * @author 장어진
 * @since 2025. 10. 10.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 10.     	장어진	          최초 생성
 *  2025. 10. 12.     	장어진	          upFolderSqn 오류 수정
 *
 * </pre>
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserFileFolderVO {

	@Include
	@NotNull
	private Integer folderSqn;

	@NotBlank(groups = InsertGroupNotDefault.class)
	private String folderNm;

	private Integer upFolderSqn;

	private String folderType;

	@NotBlank
	private String userId;

	private LocalDateTime crtDt;

	private String delYn;
}
