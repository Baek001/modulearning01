package kr.or.ddit.policies.service;

import java.util.List;

import kr.or.ddit.policies.dto.PolicyDTO;
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
public interface PoliciesService {

	/**
	 * 권한 정책 목록 조회.
	 * @return 조회 결과 없으면 list.size() == 0.
	 */
	public List<PoliciesVO> readPoliciesList();

	/**
	 * 권한 정책 단건 조회.
	 * @param featureId
	 * @return 권한정책Id, 기능Id, 설명을 담은 vo. 조회 결과 없으면 EntityNotFoundException
	 */
	public PoliciesVO readPolicies(String featureId);

	/**
	 * 권한 정책 등록.
	 * @param policyDTO
	 * @return 성공하면 true, 실패하면 false
	 */
	public boolean createPolicies(PolicyDTO policyDTO);

	/**
	 * 권한 정책 수정.
	 * @param policyDTO
	 * @return 성공하면 true, 실패하면 false
	 */
	public int modifyPolicies(PolicyDTO policyDTO);

	/**
	 * 권한 정책 삭제.
	 * @param featureId
	 * @return 성공하면 true, 실패하면 false
	 */
	public boolean removePolicies(String featureId);

	/**
	 * 해당 기능에 접근 권한이 있는지 판단
	 * @param featureId 기능 Id
	 * @return 권한 있으면 true, 없으면 false
	 */
	public boolean checkAccess(String featureId);
}
