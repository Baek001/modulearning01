package kr.or.ddit.vo;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 *
 * @author 임가영
 * @since 2025. 10. 27.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 27.     	임가영           최초 생성
 *
 * </pre>
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class MeetingRecurringBookingVO {

	@NotNull
	private Integer recurringId ; // 예약로그고유ID

	private String roomId       ;

	private String frequency    ;

	private Integer interval    ;

	private LocalDate startDate ;

	private LocalDate endDate   ;

	private Integer startTime   ;

	private Integer endTime     ;

	private String title        ;

	private String status       ;

	private String rejectReason ;

	private String userId       ;

	private LocalDate crtDt     ;

	// 요일 값들
	private List<String> weekCheckList;
	private String weekday; // db 삽입용

	// join 을 위한..
	private String userNm; // 사용자 이름
	private String roomName; // 회의실 이름
}
