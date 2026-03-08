package kr.or.ddit.policies.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import kr.or.ddit.mybatis.mapper.PoliciesDetailMapper;
import kr.or.ddit.policies.service.PoliciesDetailService;
import kr.or.ddit.vo.PoliciesDetailVO;
import lombok.RequiredArgsConstructor;

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
@Service
@RequiredArgsConstructor
public class PoliciesDetailServiceImpl implements PoliciesDetailService {

	private final PoliciesDetailMapper mapper;
	
	@Override
	public List<PoliciesDetailVO> readPoliciesDetailList() {
		return mapper.selectPoliciesDetailList();
	}

	@Override
	public List<PoliciesDetailVO> readPoliciesDetail(String featureId) {
		return mapper.selectPoliciesDetail(featureId);
	}

	@Override
	public boolean createPoliciesDetail(PoliciesDetailVO policiesDetailVO) {
		return mapper.insertPoliciesDetail(policiesDetailVO) > 0;
	}

	@Override
	public boolean modifyPoliciesDetail(PoliciesDetailVO policiesDetailVO) {
		return mapper.updatePoliciesDetailJdgdCd(policiesDetailVO) > 0;
	}

	@Override
	public boolean removePoliciesDetail(String featureId) {
		return mapper.deletePoliciesDetail(featureId) > 0;
	}

}
