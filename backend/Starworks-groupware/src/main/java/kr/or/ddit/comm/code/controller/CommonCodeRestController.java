package kr.or.ddit.comm.code.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import kr.or.ddit.comm.code.service.CommonCodeService;
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
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 26.    	임가영	       최초 생성
 *
 * </pre>
 */
@RestController
@RequestMapping("/rest/comm-code")
@RequiredArgsConstructor
public class CommonCodeRestController {
	
	private final CommonCodeService service;
	
	/**
	 * 공통코드그룹에 해당하는 모든 공통코드를 가져오는 메소드. RestController
	 * @param codeGrpId 공통코드그룹Id. 쿼리 스트링
	 * @return 조회 결과 없으면 list.size() == 0
	 */
	@GetMapping
	public List<CommonCodeVO> readCommonCodeList(@RequestParam String codeGrpId) {
		return service.readCommonCodeList(codeGrpId);
	}
	
	/**
	 * 공통코드ID 로 코드명을 찾는 메소드. RestController
	 * @param codeId 공통코드Id
	 * @return 조회 결과 없으면 EntityNotFoundException
	 */
	@GetMapping("/{codeId}")
	public CommonCodeVO readCommonCode(@PathVariable String codeId) {
		return service.readCommonCode(codeId);
	}

}
