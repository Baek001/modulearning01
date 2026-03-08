package kr.or.ddit.policies.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import kr.or.ddit.policies.service.PoliciesDetailService;
import kr.or.ddit.vo.PoliciesDetailVO;
import lombok.RequiredArgsConstructor;

/**
 * 
 * @author 임가영
 * @since 2025. 9. 26.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 26.     	임가영	       최초 생성
 *
 * </pre>
 */
@RestController
@RequestMapping("/rest/comm-policies-detail")
@RequiredArgsConstructor
public class PoliciesDetailRestController {
	
	private final PoliciesDetailService service;
	
	/**
	 * (쿼리스트링 X) 권한 정책 디테일(기능, 부서, 직급) 전체 목록 조회. RestController
	 * (쿼리스트링 O) 권한 정책 디테일 기능별로 부서와 직급 조회. RestController
	 * @return 조회 결과 없으면 list.size() == 0
	 */
	@GetMapping
	public List<PoliciesDetailVO> readPoliciesDetailList(@RequestParam(required = false) String featureId) {
		if (featureId != null && !featureId.isBlank()) {
			return service.readPoliciesDetail(featureId);
		} else {
			return service.readPoliciesDetailList();			
		}
	}

}
