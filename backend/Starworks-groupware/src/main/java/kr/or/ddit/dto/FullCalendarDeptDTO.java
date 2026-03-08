package kr.or.ddit.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 *
 * @author 장어진
 * @since 2025. 9. 26.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 26.     	장어진	          최초 생성
 *  2025. 9. 29.     	장어진	          LocalDate -> LocalDateTime 으로 수정
 *
 * </pre>
 */
@Data
public class FullCalendarDeptDTO {
	private String eventId;
	private String eventType;
	private String title;
	private LocalDateTime startDt;
	private LocalDateTime endDt;
	private String allday;
	private String description;
	private String userId;
	private String userNm;
	private String deptId;
	private String type;
}
