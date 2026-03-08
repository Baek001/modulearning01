package kr.or.ddit.vo;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.Include;

/**
 * 인사 기록을 위한 테이블
 * @author 임가영
 * @since 2025. 10. 15.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 15.     	임가영           최초 생성
 *
 * </pre>
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserHistoryVO {

	@Include
	private Integer historyId     ; //인사기록ID(시퀀스) (O)
	@NotBlank
	private String userId         ; // 대상직원ID(O)

	private String changeType     ; // 인사변동 유형코드

	private String beforeDeptId   ; // 변경전 부서ID (O)

	private String afterDeptId    ; // 변경후 부서ID (O)

	private String beforeJbgdCd   ; // 변경전 직급코드 (O)

	private String afterJbgdCd    ; // 변경후 직급코드 (O)

	private String reason         ; // 인사변경 사유

	private LocalDateTime crtDt   ; // 인사변경일 (O)

	// join 때 필요한 필드
	private String userNm;
	private String beforeDeptNm;
	private String afterDeptNm;
	private String beforeJbgdNm;
	private String afterJbgdNm;

}
