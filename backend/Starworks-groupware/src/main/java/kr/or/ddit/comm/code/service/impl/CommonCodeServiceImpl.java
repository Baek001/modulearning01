package kr.or.ddit.comm.code.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import kr.or.ddit.comm.code.service.CommonCodeService;
import kr.or.ddit.comm.exception.EntityNotFoundException;
import kr.or.ddit.mybatis.mapper.CommonCodeMapper;
import kr.or.ddit.vo.CommonCodeVO;
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
 *    수정일      		수정자              수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 26.     	임가영	          최초 생성
 *
 * </pre>
 */
@Service
@RequiredArgsConstructor
public class CommonCodeServiceImpl implements CommonCodeService {
	
	@Autowired
	private CommonCodeMapper mapper;

	/**
	 * 공통코드그룹에 해당하는 모든 공통코드를 가져오는 메소드
	 */
	@Override
	public List<CommonCodeVO> readCommonCodeList(String codeGrpId) {
		return mapper.selectCommonCodeList(codeGrpId);
	}

	/**
	 * 공통코드ID 로 코드명을 찾는 메소드
	 */
	@Override
	public CommonCodeVO readCommonCode(String codeId) {
		CommonCodeVO commonCode = mapper.selectCommonCode(codeId);
		if(commonCode == null) {
			throw new EntityNotFoundException(commonCode);
		}
		return commonCode;
	}

}
