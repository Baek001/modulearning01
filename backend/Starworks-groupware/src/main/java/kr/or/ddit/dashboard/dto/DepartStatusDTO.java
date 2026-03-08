package kr.or.ddit.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 부서의 현재 현황을 대시보드로 넘겨주기 위한 DTO
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
public class DepartStatusDTO {

	// 전체 팀원 수
	Integer departCnt;

	// 현재 근무 중 팀원 수
	Integer workingUserCnt;

	// 휴가 팀원 수
	Integer vacationUserCnt;

	// 외근 팀원 수
	Integer businessTripCnt;

}
