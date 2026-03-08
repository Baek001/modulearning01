package kr.or.ddit.businesstrip.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kr.or.ddit.businesstrip.service.BusinessTripService;
import kr.or.ddit.vo.BusinessTripVO;
import lombok.RequiredArgsConstructor;

/**
 * 
 * @author 윤서현
 * @since 2025. 9. 26.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 26.     	윤서현	          최초 생성
 *
 * </pre>
 */
@RestController
@RequestMapping("/rest/approval-businesstrip")
@RequiredArgsConstructor
public class BusinessTripRestController {

	private final BusinessTripService service;
	
	/**
	 * 전자결재 출장일정 캘린더에서 목록 조회. RestController
	 * @return
	 */
	@GetMapping
	public List<BusinessTripVO> readBusinessTripList(){
		return service.readBusinessTripList();
	}
	
	/**
	 * 전자결재 출장일정 캘린더에서 상세조회. RestController
	 * @param bztrSqn
	 * @return
	 */
	@GetMapping("/{bztrSqn}")
	public BusinessTripVO readBusinessTrip(@PathVariable String bztrSqn) {
		return service.readBusinessTrip(bztrSqn);
	}
}
