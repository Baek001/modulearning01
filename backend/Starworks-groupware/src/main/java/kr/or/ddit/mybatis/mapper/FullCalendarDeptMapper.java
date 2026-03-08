package kr.or.ddit.mybatis.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import kr.or.ddit.dto.FullCalendarDeptDTO;

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
 *  2025. 9. 27.     	장어진	          @Param을 @RequestBody FullCalendarDeptDTO로 변경
 *
 * </pre>
 */
@Mapper
public interface FullCalendarDeptMapper {
	/**
	 * FullCalendar Department 전체 조회.
	 * @param dto : FullCalendarDeptDTO 객체
	 * @return 조회 결과 없으면 list.size() == 0
	 */
	public List<FullCalendarDeptDTO> selectFullCalendarDeptList(FullCalendarDeptDTO dto);
}
