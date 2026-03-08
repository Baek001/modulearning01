package kr.or.ddit.calendar.dept.service;

import java.util.List;

import org.springframework.stereotype.Service;

import kr.or.ddit.dto.FullCalendarDeptDTO;
import kr.or.ddit.mybatis.mapper.FullCalendarDeptMapper;
import lombok.RequiredArgsConstructor;

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
@Service
@RequiredArgsConstructor
public class FullCalendarDeptServiceImpl implements FullCalendarDeptService {
	private final FullCalendarDeptMapper mapper;

	@Override
	public List<FullCalendarDeptDTO> readFullCalendarDeptList(FullCalendarDeptDTO dto) {
		return mapper.selectFullCalendarDeptList(dto);
	}
}
