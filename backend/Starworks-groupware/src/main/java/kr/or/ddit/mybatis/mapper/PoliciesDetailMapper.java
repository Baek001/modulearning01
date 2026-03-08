package kr.or.ddit.mybatis.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import kr.or.ddit.vo.PoliciesDetailVO;

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
public interface PoliciesDetailMapper {
	
	/**
	 * 권한 정책 디테일(기능, 부서, 직급) 전체 목록 조회.
	 * @return 조회 결과 없으면 list.size() == 0
	 */
	public List<PoliciesDetailVO> selectPoliciesDetailList();
	
	/**
	 * 권한 정책 디테일 기능별로 부서와 직급 조회.
	 * @param featureId 기능 Id
	 * @return 조회 결과 없으면 list.size() == 0
	 */
	public List<PoliciesDetailVO> selectPoliciesDetail(String featureId);
	
	/**
	 * 권한 정책 디테일(기능, 부서, 직급) 등록.
	 * @param policiesDetail
	 * @return 성공한 레코드 수
	 */
	public int insertPoliciesDetail(PoliciesDetailVO policiesDetail);
	
	/**
	 * 권한 정책 직급 코드 수정.
	 * @param policiesDetailVO 기능과 수정할 직급 정보를 담은 vo
	 * @return 성공한 레코드 수
	 */
	public int updatePoliciesDetailJdgdCd(PoliciesDetailVO policiesDetail);
	
	/**
	 * 권한 정책 삭제.
	 * @param featureId 삭제할 기능 Id
	 * @return 성공한 레코드 수
	 */
	public int deletePoliciesDetail(String featureId);
}
