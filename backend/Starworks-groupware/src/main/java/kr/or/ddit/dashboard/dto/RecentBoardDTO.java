package kr.or.ddit.dashboard.dto;

import java.time.LocalDateTime;

import kr.or.ddit.vo.UsersVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 공지사항 정보를 대시보드에 띄우기 위한 DTO
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
public class RecentBoardDTO {

	// 게시글 Id
	private String pk;

	// 게시글 제목
	private String boardTitle;

	// 작성자
	private UsersVO crtUserVO;

	// 작성일
	private LocalDateTime crtDt;

}
