package kr.or.ddit.mybatis.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import kr.or.ddit.dto.FullCalendarTeamDTO;

/**
 *
 * @author 장어진
 * @since 2025. 9. 30.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 30.     	장어진	          최초 생성
 *  2025. 9. 30.     	장어진	          파라미터 수정
 *
 * </pre>
 */
@Mapper
public interface FullCalendarTeamMapper {
	/**
	 * FullCalendar Team 전체 조회.
	 * @param dto : FullCalendarTeamDTO 객체
	 * @return 조회 결과 없으면 list.size() == 0
	 */
	public List<FullCalendarTeamDTO> selectFullCalendarTeamList(@Param("userId") String userId);
}
