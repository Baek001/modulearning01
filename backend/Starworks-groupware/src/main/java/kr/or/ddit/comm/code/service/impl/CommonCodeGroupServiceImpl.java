package kr.or.ddit.comm.code.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import kr.or.ddit.comm.code.service.CommonCodeGroupService;
import kr.or.ddit.comm.exception.EntityNotFoundException;
import kr.or.ddit.mybatis.mapper.CommonCodeGroupMapper;
import kr.or.ddit.vo.CommonCodeGroupVO;
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
 *   수정일      			수정자               수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 26.     	임가영	          최초 생성
 *
 * </pre>
 */
@Service
@RequiredArgsConstructor
public class CommonCodeGroupServiceImpl implements CommonCodeGroupService {
	
	private final CommonCodeGroupMapper mapper;
	
	/**
	 * 공통코드그룹 전체 목록 조회
	 */
	@Override
	public List<CommonCodeGroupVO> readCommonCodeGroupList() {
		return mapper.selectCommonCodeGroupList();
	}

	/**
	 * 공통코드그룹 단건 조회
	 */
	@Override
	public CommonCodeGroupVO readCommonCodeGroup(String codeGrpId) {
		CommonCodeGroupVO commonCodeGroup = mapper.selectCommonCodeGroup(codeGrpId);
		if(commonCodeGroup == null) {
			throw new EntityNotFoundException(codeGrpId);
		}
		return commonCodeGroup;
	}

}
