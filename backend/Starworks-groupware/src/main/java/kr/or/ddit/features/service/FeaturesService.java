package kr.or.ddit.features.service;

import java.util.List;

import kr.or.ddit.vo.FeaturesVO;

/**
 * 
 * @author 임가영
 * @since 2025. 9. 25.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 25.     	임가영	       최초 생성
 *
 * </pre>
 */
public interface FeaturesService {

	/**
	 * 기능 목록 전체 조회.
	 * @return
	 */
	public List<FeaturesVO> readFeaturesList();
	
	/**
	 * 기능 한 건 조회.
	 * @param featureId 기능 Id
	 * @return 실패하면 EntityNotFoundException
	 */
	public FeaturesVO readFeatures(String featureId);
} 
