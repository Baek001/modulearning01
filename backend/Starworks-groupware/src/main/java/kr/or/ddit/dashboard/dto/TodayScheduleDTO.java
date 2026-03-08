package kr.or.ddit.dashboard.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 대시보드에 오늘의 일정 띄우기 위한 DTO
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
@NoArgsConstructor
@AllArgsConstructor
public class TodayScheduleDTO {

	// 일정 pk(Id)
	private String pk;

	// 시작 시각, 만약 하루종일이면 '하루종일' 이 들어감
	private LocalDateTime startTime;

	// 종료 시각
	private LocalDateTime endTime;

	// 일정 이름
	private String scheduleNm;

	// 개인일정 / 부서일정 / 프로젝트팀일정
	private String scheduleType;

	// 일정 등록자명
	private String userNm;

	// 등록자 직급명
	private String jbgdNm;

	// 등록자 부서명
	private String deptNm;

	// 일정 설명
	private String description;
}
