package kr.or.ddit.vo;

import java.time.LocalDate;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kr.or.ddit.comm.validate.UpdateGroup;
import kr.or.ddit.comm.validate.UpdateGroupNotDefault;
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
public class MeetingReservationVO {

	@NotNull(groups = UpdateGroup.class)
	private Integer reservationId;
	@NotBlank
	private String roomId;
	private String userId;
	@NotNull
	private Integer startTime;
	@NotNull
	private Integer endTime;
	@Size(max=85)
	private String title;
	private LocalDate meetingDate;
	private Integer recurringId;

	// join 을 위한 필드
	private String roomName;
	private String userNm;
	private String deptNm;
	private String filePath;

	// rownum 필드
	private Integer rnum;

	// 관리자에서 보여줄건지 사용자에서 보여줄건지 여부
	private boolean admin;

	@AssertTrue(message = "회의 시작 시간은 종료 시간보다 빨라야 합니다.")
	public boolean isStartBeforeEnd() {
	    if (startTime == null || endTime == null) return true;
	    return startTime < endTime;
	}
}
