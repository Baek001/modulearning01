package kr.or.ddit.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 *
 * @author 장어진
 * @since 2025. 10. 15.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 15.     	장어진	          최초 생성
 *  2025. 10. 16.     	장어진	          컬럼 추가
 *
 * </pre>
 */
@Data
public class AttendanceDepartStatusDTO {
	private String workYmd;
	private String userId;
	private String userNm;
	private String jbgdCd;
	private LocalDateTime workBgngDt;
	private LocalDateTime workEndDt;
	private Integer workHr;
	private String lateYn;
	private String earlyYn;
	private String overtimeYn;
	private Integer overtimeHr;
	private String deptId;
	private String workSttsCd;

	private String vactYn;
	private String bztrYn;
}
