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
 *  2025. 9. 30.     	장어진	          요소 입력 및 Project -> Team 으로 이름 변경
 *
 * </pre>
 */
@Data
public class FullCalendarTeamDTO {
	private String eventId;
	private String eventType;
	private String title;
	private LocalDateTime startDt;
	private LocalDateTime endDt;
	private String allday;
	private String description;
	private String userId;
	private String userNm;
	private String bizId;
	private String type;
}
