package kr.or.ddit.vacation.service;

import java.util.List;

import org.springframework.stereotype.Service;

import kr.or.ddit.comm.exception.EntityNotFoundException;
import kr.or.ddit.mybatis.mapper.VactionMapper;
import kr.or.ddit.vo.VactionVO;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author 윤서현
 * @since 2025. 9. 25.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 25.     	윤서현	          최초 생성
 *  2025. 10. 23.     	장어진            연차 계산 기능 추가
 *
 * </pre>
 */
@Service
@RequiredArgsConstructor
public class VactionServiceImpl implements VactionService{

	private final VactionMapper mapper;

	@Override
	public boolean createVaction(VactionVO vac) {
		return mapper.insertVaction(vac) > 0;
	}

	@Override
	public List<VactionVO> readVactionList() {
		return mapper.selectVactionList();
	}

	@Override
	public VactionVO readVaction(String vactSqn) {
		VactionVO vac = mapper.selectVacation(vactSqn);
		if(vac == null) {
			throw new EntityNotFoundException(vac);
		}
		return vac;
	}

	@Override
	public int readVacationDaysByUser(String vactUserId, String vactCd, Integer year) {
		return mapper.selectVacationDaysbyUser(vactUserId, vactCd, year);
	}
}
