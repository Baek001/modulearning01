package kr.or.ddit.policies.service.impl;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.comm.exception.EntityNotFoundException;
import kr.or.ddit.mybatis.mapper.PoliciesDetailMapper;
import kr.or.ddit.mybatis.mapper.PoliciesMapper;
import kr.or.ddit.mybatis.mapper.UsersMapper;
import kr.or.ddit.policies.dto.PolicyDTO;
import kr.or.ddit.policies.service.PoliciesService;
import kr.or.ddit.vo.PoliciesDetailVO;
import kr.or.ddit.vo.PoliciesVO;
import kr.or.ddit.vo.UsersVO;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author 임가영
 * @since 2025. 10. 12.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           	  수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 12.     	임가영	          최초 생성
 *
 * </pre>
 */
@Service
@RequiredArgsConstructor
public class PoliciesServiceImpl implements PoliciesService {

	private final PoliciesMapper mapper;
	private final PoliciesDetailMapper detailMapper;
	private final UsersMapper usersMapper;


	@Override
	public List<PoliciesVO> readPoliciesList() {
		return mapper.selectPoliciesList();
	}

	@Override
	public PoliciesVO readPolicies(String featureId) {
		PoliciesVO policy = mapper.selectPolicies(featureId);
		if(policy == null) {
			throw new EntityNotFoundException(policy);
		}
		return policy;
	}

	/**
	 * 권한 정책 등록
	 */
	@Override
	public boolean createPolicies(PolicyDTO policyDTO) {
		boolean success = false;

		String featureId = policyDTO.getFeatureId();
		String remark = policyDTO.getRemark();
		String jbgdCd = policyDTO.getJbgdCd();
		List<String> deptList=  policyDTO.getDeptList();

		// 정책 테이블 insert
		PoliciesVO policyVO = new PoliciesVO();
		policyVO.setFeatureId(featureId);
		policyVO.setRemark(remark);

		int rowcnt1 = mapper.insertPolicies(policyVO);
		int rowcnt2 = 0;

		// 정책 detail 테이블 insert
		for (String deptId : deptList) {
			PoliciesDetailVO detailVO = new PoliciesDetailVO();
			detailVO.setFeatureId(featureId);
			detailVO.setJbgdCd(jbgdCd);
			detailVO.setDeptId(deptId);

			rowcnt2 += detailMapper.insertPoliciesDetail(detailVO);
		}

		if (rowcnt1 + rowcnt2 == deptList.size() + 1) {
			success = true;
		}

		return success;
	}

	/**
	 * 권한 정책 수정
	 */
	@Override
	@Transactional
	public int modifyPolicies(PolicyDTO policyDTO) {

		String featureId = policyDTO.getFeatureId();
		String remark = policyDTO.getRemark();
		String jbgdCd = policyDTO.getJbgdCd();
		List<String> deptList=  policyDTO.getDeptList();

		// 정책 테이블 update
		PoliciesVO policyVO = new PoliciesVO();
		policyVO.setFeatureId(featureId);
		policyVO.setRemark(remark);

		int rowcnt1 = mapper.updatePolicies(policyVO);

		// 정책 디테일 테이블 delete & insert
		int rowcnt2 = detailMapper.deletePoliciesDetail(featureId);

		int rowcnt3 = 0;
		for (String deptId : deptList) {
			PoliciesDetailVO detailVO = new PoliciesDetailVO();
			detailVO.setFeatureId(featureId);
			detailVO.setJbgdCd(jbgdCd);
			detailVO.setDeptId(deptId);

			rowcnt3 = detailMapper.insertPoliciesDetail(detailVO);
		}

		mapper.updatePoliciesModDt(policyVO);

		return rowcnt1 + rowcnt2 + rowcnt3;
	}

	/**
	 * 권한 정책 삭제
	 */
	@Override
	public boolean removePolicies(String featureId) {
		boolean success = false;

		// detail Policies 먼저 지우기
		int rowcnt1 = detailMapper.deletePoliciesDetail(featureId);

		// policies 지우기
		int rowcnt2 = mapper.deletePolices(featureId);

		if ((rowcnt1 + rowcnt2) >= 2) {
			success = true;
		} else {
			success = false;
		}

		return success;
	}

	/**
	 * 해당 기능에 접근 권한이 있는지 판단
	 */
	@Override
	public boolean checkAccess(String featureId) {

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		String username = authentication.getName();
		UsersVO user = usersMapper.selectUser(username);
		String userDeptId = user.getDeptId();
		String userUpDeptId = user.getUpDeptId();

		// 권한 정책 가져오기
		PoliciesVO policiesVO = mapper.selectPolicies(featureId);
		if(policiesVO == null) return true; // 권한 정책이 등록되지 않음

		// 권한 디테일 (부서, 직급 등) 가져오기
		List<PoliciesDetailVO> policiesDetailList = policiesVO.getPoliciesDetailList();
		String policiesJbgd = policiesDetailList.get(0).getJbgdCd();

		int userLevel = Integer.parseInt(user.getJbgdCd().substring(5)); // 내 직급
		int policyLevel = Integer.parseInt(policiesJbgd.substring(5)); // 접근 가능한 직급

		// 권한 정책보다 사용자 직급이 낮을 때
		if(userLevel > policyLevel) {
			return false;
		} else {
			// 허용 직급 이상이라면 부서 비교
			for(PoliciesDetailVO detail : policiesDetailList) {
				// 만약 내 부서(상위부서 포함)가 있다면..
				if (detail.getDeptId().equals(userDeptId) || detail.getDeptId().equals(userUpDeptId)) {
					return true;
				}
			}
		}
		return false;
	}



}
