package kr.or.ddit.businesstrip.service;

import java.util.List;

import org.springframework.stereotype.Service;

import kr.or.ddit.comm.exception.EntityNotFoundException;
import kr.or.ddit.mybatis.mapper.BusinessTripMapper;
import kr.or.ddit.vo.BusinessTripVO;
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
 *
 * </pre>
 */
@Service
@RequiredArgsConstructor
public class BusinessTripServiceImpl implements BusinessTripService{

	private final BusinessTripMapper mapper;
	
	@Override
	public boolean createBusinessTrip(BusinessTripVO busTrip) {
		return mapper.insertBusinessTrip(busTrip) > 0;
	}

	@Override
	public List<BusinessTripVO> readBusinessTripList() {
		return mapper.selectBusinessTripList();
	}

	@Override
	public BusinessTripVO readBusinessTrip(String bztrSqn) {
		BusinessTripVO busTrip = mapper.selectBusinessTrip(bztrSqn);
		if(busTrip == null) {
			throw new EntityNotFoundException(busTrip);
		}
		return busTrip;
	}

}
