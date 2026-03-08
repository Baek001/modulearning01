package kr.or.ddit.approval.document.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import kr.or.ddit.approval.document.service.AuthorizationDocumentService;
import kr.or.ddit.vo.AuthorizationDocumentVO;
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
 *  2025.10. 04.		임가영		   modifyAuthorizationDocumentSign(htmlData 업데이트) 메소드 추가
 *
 * </pre>
 */
@CrossOrigin(origins = "*") // 테스트용
@RestController
@RequiredArgsConstructor
public class AuthorizationDocumentRestController {

	private final AuthorizationDocumentService service;

	//private final AuthorizationDocumentService docService;

	/**
	 * 본인이 기안한 문서 중, 완료된 문서 확인 RestController
	 * @param atrzUserId 기안자 Id
	 * @return 조회 결과 없으면 list.size() == 0
	 */
//	@GetMapping("/rest/approval-document/user")
//	public List<AuthorizationDocumentVO> readAuthorizationDocumentListUser(
//		Authentication authentication
//	) {
//		String username = authentication.getName();
//		return service.readAuthorizationDocumentListUser(username);
//	}

	/**
	 * 본인 부서의 모든 완료된 문서 확인 RestController
	 * @param userId 사용자Id
	 * @return 조회 결과 없으면 list.size() == 0
	 */
//	@GetMapping("/rest/approval-document/depart")
//	public List<AuthorizationDocumentVO> readAuthorizationDocumentListDepart(
//		Authentication authentication
//	) {
//		String username = authentication.getName();
//		return service.readAuthorizationDocumentListDepart(username);
//	}


	/**
	 *  결재 승인 시 도장 찍히고 그 htmlData 가 update 되는 로직
	 * @param authorizationDocument htmlData 와 DocId(결재문서일련번호) 가 들어있는 vo
	 * @return
	 */
	@PutMapping("/rest/approval-document/sign")
	public Map<String, Object> modifyAuthorizationDocumentSign(
		@RequestBody AuthorizationDocumentVO authorizationDocument
	) {
		boolean sucess= service.modifyAuthorizationDocumentSign(authorizationDocument);

		return Map.of("success", sucess);
	}
    /**
     * Ajax로 문서번호를 가져오는 엔드포인트
     * @return 예: ATRZ000000001234
     */
    @GetMapping("/rest/getNextDocId")
    @ResponseBody
    public String getNextDocId() {
        return service.getNextDocId();
    }

    @GetMapping("/rest/approval/approvalCount")
    @ResponseBody
    public Map<String, Object> getApprovalCount(){
    	int count = service.getApprovalCount();
    	return Map.of("approvalCount", count);
    }

}
