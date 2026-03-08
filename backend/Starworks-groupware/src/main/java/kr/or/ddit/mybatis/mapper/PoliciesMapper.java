package kr.or.ddit.mybatis.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import kr.or.ddit.vo.PoliciesVO;

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
public interface PoliciesMapper {

	/**
	 * 권한 정책 목록 조회.
	 * @return
	 */
	public List<PoliciesVO> selectPoliciesList();

	/**
	 * 권한 정책 단건 조회.
	 * @param policyId 권한정책 Id
	 * @return 권한정책Id, 기능Id, 설명을 담은 vo
	 */
	public PoliciesVO selectPolicies(String featureId);

	/**
	 * 권한 정책 등록.
	 * @param polices
	 * @return 성공한 레코드 수
	 */
	public int insertPolicies(PoliciesVO policy);

	/**
	 * 권한 정책 수정.
	 * @param polices
	 * @return 성공한 레코드 수
	 */
	public int updatePolicies(PoliciesVO policy);

	/**
	 * 권한 정책 수정시, 수정일 update
	 * @return 성공한 레코드 수
	 */
	public int updatePoliciesModDt(PoliciesVO policy);

	/**
	 * 권한 정책 삭제.
	 * @param policyId
	 * @return 성공한 레코드 수
	 */
	public int deletePolices(String featureId);
}
