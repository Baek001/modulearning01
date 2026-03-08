package kr.or.ddit.businesstrip.service;

import java.util.List;

import kr.or.ddit.vo.BusinessTripVO;

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
public interface BusinessTripService {

	/**
	 * 전자결재 기안서 일정 캘린더 등록
	 * @param busTrip
	 * @return
	 */
	public boolean createBusinessTrip(BusinessTripVO busTrip);
	/**
	 * 전자결재 출장일정 캘린더 목록 조회
	 * @return
	 */
	public List<BusinessTripVO> readBusinessTripList();
	/**
	 * 전자결재 출장일정 캘린더 목록 상세조회
	 * @param bztrSqn
	 * @return
	 */
	public BusinessTripVO readBusinessTrip(String bztrSqn);
}
