package kr.or.ddit.comm.code.service;

import java.util.List;

import kr.or.ddit.vo.CommonCodeVO;

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
public interface CommonCodeService {

	/**
	 * 공통코드그룹에 해당하는 모든 공통코드를 가져오는 메소드
	 * @param codeGrpId 공통코드그룹Id
	 * @return 조회 결과 없으면 list.size() == 0
	 */
	public List<CommonCodeVO> readCommonCodeList(String codeGrpId);
	
	/**
	 * 공통코드ID 로 코드명을 찾는 메소드
	 * @param codeId 공통코드Id
	 * @return 조회 결과 없으면 EntityNotFoundException
	 */
	public CommonCodeVO readCommonCode(String codeId);
}
