package kr.or.ddit.mybatis.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

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
@Mapper
public interface BusinessTripMapper {
	
	/**
	 * 전자결재 기안서 일정 캘린더 등록
	 * @param busTrip
	 * @return
	 */
	public int insertBusinessTrip(BusinessTripVO busTrip);
	/**
	 * 전자결재 출장일정 캘린더 목록 조회
	 * @return
	 */
	public List<BusinessTripVO> selectBusinessTripList();
	/**
	 * 전자결재 출장일정 캘린더 목록 상세조회
	 * @param bztrSqn
	 * @return
	 */
	public BusinessTripVO selectBusinessTrip(String bztrSqn);
}
