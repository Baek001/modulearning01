package kr.or.ddit.features.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kr.or.ddit.features.service.FeaturesService;
import kr.or.ddit.vo.FeaturesVO;
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
@RequestMapping("/rest/comm-features")
@RequiredArgsConstructor
public class FeaturesRestController {

	private final FeaturesService service;
	
	/**
	 * 기능 목록 전체 조회. RestController
	 * @return
	 */
	@GetMapping
	public List<FeaturesVO> readFeaturesList() {
		return service.readFeaturesList();
	}
	
	/**
	 * 기능 한 건 조회. RestController
	 * @param featureId 기능 Id
	 * @return 실패하면 EntityNotFoundException
	 */
	@GetMapping("/{featureId}")
	public FeaturesVO readFeatures(@PathVariable String featureId) {
		return service.readFeatures(featureId);
	}
}
