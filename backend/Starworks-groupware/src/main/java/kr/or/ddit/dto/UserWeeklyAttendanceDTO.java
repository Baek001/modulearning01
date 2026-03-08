package kr.or.ddit.dto;

import java.time.LocalDate;

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
public class UserWeeklyAttendanceDTO {
	private String userId;
	private String userNm;
	private String deptNm;
	private String jbgdNm;
	private LocalDate workWeekStartDate;
	private Integer workDays;
	private Integer totalWorkHr;
	private Integer lateCount;
	private Integer earlyCount;
	private Integer overtimeCount;
	private Integer totalOvertimeHr;
}
