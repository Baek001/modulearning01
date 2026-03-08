package kr.or.ddit.dashboard.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 대시보드에 진행중인 프로젝트 정보를 띄우기 위한 DTO
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
public class CurrentProjectDTO {

	// 프로젝트 Id
	private String pk;

	// 프로젝트 이름
	private String projectNm;

	// 시작일
	private LocalDateTime startDate;

	// 종료일
	private LocalDateTime endDate;

	// 진행률
	private Integer progress;
}
