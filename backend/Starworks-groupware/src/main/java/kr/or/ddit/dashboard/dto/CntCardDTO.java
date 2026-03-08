package kr.or.ddit.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 메인 화면 카드에 카운트를 넣기 위한 DTO
 * @author 임가영
 * @since 2025. 10. 15.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 15.     		임가영           최초 생성
 *
 * </pre>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CntCardDTO {

	// 진행 중인 업무 수
	private Integer mainTaskCnt;

	// 처리 대기 중인 결재 수
	private Integer waitApprovalCnt;

	// 이번 주 일정(개인일정 + 부서일정 + 팀일정 포함?)
	private Integer weekScheduleCnt;

	// 읽지 않은 메일 수
	private Integer unreadMailCnt;

}
