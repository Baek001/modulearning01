package kr.or.ddit.calendar.team.service;

import java.util.List;

import org.springframework.stereotype.Service;

import kr.or.ddit.dto.FullCalendarTeamDTO;
import kr.or.ddit.mybatis.mapper.FullCalendarTeamMapper;
import lombok.RequiredArgsConstructor;

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
@Service
@RequiredArgsConstructor
public class FullCalendarTeamServiceImpl implements FullCalendarTeamService {
	private final FullCalendarTeamMapper mapper;

	@Override
	public List<FullCalendarTeamDTO> readFullCalendarTeamList(String userId) {
		return mapper.selectFullCalendarTeamList(userId);
	}

}
