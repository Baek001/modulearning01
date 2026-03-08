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
 *	2025. 10.01.		홍현택			결재자이름 프로퍼티 추가
 * </pre>
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AuthorizationLineVO {

	@Include
	@NotBlank
	private Integer atrzLineSqn;

	@Include
	@NotBlank
	private String atrzDocId;

	private Integer atrzLineSeq;

	@NotBlank
	private String atrzApprUserId;
	private String atrzApprStts;
	private LocalDateTime prcsDt;
	private String signFileId;
	private String atrzOpnn;
	private String atrzAct;
	private String apprAtrzYn;

	//임시저장
	private String atrzTempSqn;

	// =================== 조인으로 가져올 결재자정보 ===================
    private String atrzApprUserNm; // 결재자 이름
    private String jbgdNm; // 결재자 직급명
    private String deptNm; // 결재자 부서명
    private String filePath; // 사용자 프로필사진 파일 Id
}
