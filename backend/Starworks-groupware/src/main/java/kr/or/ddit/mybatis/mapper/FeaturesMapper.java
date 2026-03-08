package kr.or.ddit.mybatis.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

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
@Mapper
public interface FeaturesMapper {
	
	/**
	 * 기능 목록 전체 조회.
	 * @return
	 */
	public List<FeaturesVO> selectFeaturesList();
	
	/**
	 * 기능 한 건 조회.
	 * @param featureId 기능 Id
	 * @return 기능 Id, 기능명
	 */
	public FeaturesVO selectFeatures(String featureId);

}
