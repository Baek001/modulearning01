package kr.or.ddit.comm.code.service;

import java.util.List;

import kr.or.ddit.vo.CommonCodeGroupVO;

/**
 * 
 * @author 임가영
 * @since 2025. 9. 26.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 26.     	임가영	          최초 생성
 *
 * </pre>
 */
public interface CommonCodeGroupService {

	/** 
	 * 공통코드그룹 전체 목록 조회
	 * @return 조회 결과 없으면 list.size() == 0
	 */
	public List<CommonCodeGroupVO> readCommonCodeGroupList();
	
	/**
	 * 공통코드그룹 단건 조회
	 * @param codeGrpId 공통코드그룹Id
	 * @return 공통코드그룹명, 사용여부가 담긴 vo, 조회 결과 없으면 EntityNotFoundException
	 */
	public CommonCodeGroupVO readCommonCodeGroup(String codeGrpId);
}
