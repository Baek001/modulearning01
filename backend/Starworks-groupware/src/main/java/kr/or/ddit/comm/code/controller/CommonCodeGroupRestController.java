package kr.or.ddit.comm.code.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kr.or.ddit.comm.code.service.CommonCodeGroupService;
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
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 26.     	임가영	       최초 생성
 *
 * </pre>
 */
@RestController
@RequestMapping("/rest/comm-codegroup")
@RequiredArgsConstructor
public class CommonCodeGroupRestController {
	
	private final CommonCodeGroupService service;
	
	/** 
	 * 공통코드그룹 전체 목록 조회. RestController
	 * @return 조회 결과 없으면 list.size() == 0
	 */
	@GetMapping
	public List<CommonCodeGroupVO> readCommonCodeGroupList() {
		return service.readCommonCodeGroupList();
	}
	
	/**
	 * 공통코드그룹 단건 조회. RestController
	 * @param codeGrpId 공통코드그룹Id
	 * @return 공통코드그룹명, 사용여부가 담긴 vo, 조회 결과 없으면 EntityNotFoundException
	 */
	@GetMapping("/{codeGrpId}")
	public CommonCodeGroupVO readCommonCodeGroup(@PathVariable String codeGrpId) {
		return service.readCommonCodeGroup(codeGrpId);
	}
}
