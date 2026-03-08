package kr.or.ddit.approval.document.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import kr.or.ddit.approval.document.service.AuthorizationDocumentService;
import kr.or.ddit.approval.service.AuthorizationDocumentPdfService;
import kr.or.ddit.comm.exception.DocumentAccessDeniedException;
import kr.or.ddit.comm.file.service.FileDetailService;
import kr.or.ddit.comm.paging.PaginationInfo;
import kr.or.ddit.comm.paging.SimpleSearch;
import kr.or.ddit.comm.paging.renderer.MazerPaginationRenderer;
import kr.or.ddit.comm.paging.renderer.PaginationRenderer;
import kr.or.ddit.security.CustomUserDetails;
import kr.or.ddit.vo.AuthorizationDocumentPdfVO;
import kr.or.ddit.vo.AuthorizationDocumentVO;
import kr.or.ddit.vo.FileDetailVO;
import lombok.RequiredArgsConstructor;

/**
 * 전결 문서함 Read Controller
 * @author 임가영
 * @since 2025. 10. 1.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 1.     	임가영           최초 생성
 *  2025. 10. 3.		임가영			보관함 상세화면 컨트롤러-뷰 연결
 *  2025. 10.24.		홍현택			left-menu 카운트 표시를 위한 카운트 모델에 담기
 *  2025. 10.24.		홍현택			카운트 로직 분리
 *  2025. 10.25.		홍현택			handleDocumentAccessDeniedException 추가
 * </pre>
 */
@Controller
@RequiredArgsConstructor
public class AuthorizationDocumentArchiveReadController {

	private final AuthorizationDocumentService service;
	private final AuthorizationDocumentPdfService documentPdfService;
	private final FileDetailService fileDetailService;

	private static final int screenSize = 10;
    private static final int blockSize = 5;

	/**
     * 개인 결재 보관함 페이지로 이동합니다.
     * @param model
     * @return "approval/approvalarchive"
     */
    @GetMapping("/approval/archive/personal")
    public String approvalArchivePersonal(
    	@RequestParam(required = false, defaultValue = "1") int page,
    	@ModelAttribute("search") SimpleSearch search,
    	@RequestParam(required = false) String atrzDocTmplId,
    	Authentication authentication,
    	Model model
    ) {

    	// 현재 페이지 설정
    	PaginationInfo<AuthorizationDocumentVO> paging = new PaginationInfo<>(screenSize, blockSize);
    	paging.setCurrentPage(page);
    	paging.setSimpleSearch(search); // simpleSearch 세팅

    	// 현재 로그인된 사용자 정보 가져오기
    	String username = authentication.getName();
    	AuthorizationDocumentVO avo = new AuthorizationDocumentVO();
    	avo.setAtrzUserId(username);
    	avo.setAtrzDocTmplId(atrzDocTmplId); // 검색을 위한 기안템플릿ID 세팅

    	// 기안자 Id 의 결재 문서중 , 현재 결재 단계 코드 : 최종결재 인 List 조회
    	List<AuthorizationDocumentVO> authorizationDocumentList = service.readAuthorizationDocumentListUser(avo, paging);

    	// paginationRenderer 로 Page UI HTML 생성
    	PaginationRenderer renderer = new MazerPaginationRenderer();
    	String pagingHTML = renderer.renderPagination(paging);

    	model.addAttribute("approvalList", authorizationDocumentList);
    	model.addAttribute("filter", "personal");
    	model.addAttribute("pagingHTML", pagingHTML);
    	model.addAttribute("atrzDocTmplId", atrzDocTmplId);

        return "approval/approvalarchive";
    }

    /**
     * 부서 결재 보관함 페이지로 이동합니다.
     * @param model
     * @return "approval/approvalarchive"
     */
    @GetMapping("/approval/archive/depart")
    public String approvalArchiveDepart(
    	@RequestParam(required = false, defaultValue = "1") int page,
    	@ModelAttribute("search") SimpleSearch search,
    	@RequestParam(required = false) String atrzDocTmplId,
    	Authentication authentication,
    	Model model
    ) {

    	// 현재 페이지 설정
    	PaginationInfo<AuthorizationDocumentVO> paging = new PaginationInfo<>(screenSize, blockSize);
    	paging.setCurrentPage(page);
    	paging.setSimpleSearch(search);

    	// 현재 로그인된 사용자 정보 가져오기
    	String username = authentication.getName();
    	AuthorizationDocumentVO avo = new AuthorizationDocumentVO();
    	avo.setAtrzUserId(username);
    	avo.setAtrzDocTmplId(atrzDocTmplId);

        // 사용자 부서의 현재 결재 단계 코드 : 최종결재 인 List 조회
    	List<AuthorizationDocumentVO> authorizationDocumentList = service.readAuthorizationDocumentListDepart(avo, paging);

    	// paginationRenderer 로 Page UI HTML 생성
    	PaginationRenderer renderer = new MazerPaginationRenderer();
    	String pagingHTML = renderer.renderPagination(paging);

    	model.addAttribute("approvalList", authorizationDocumentList);
    	model.addAttribute("filter", "depart");
    	model.addAttribute("pagingHTML", pagingHTML);
    	model.addAttribute("atrzDocTmplId", atrzDocTmplId);

        return "approval/approvalarchive";
    }

    /**
     * 결재함 상세조회
     * @param atrzDocId
     * @param authentication
     * @param model
     * @return
     */
    @GetMapping("/approval/archive/detail/{atrzDocId}")
    public String approvalArchiveDetail(
		@PathVariable String atrzDocId,
        Authentication authentication,
        Model model
    ) {
    	 // 로그인 사용자 정보
        String loginId = authentication.getName();
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails userDetail) {
            loginId = userDetail.getUsername();
        }

        AuthorizationDocumentVO vo = service.readAuthDocument(atrzDocId, loginId);
        List<FileDetailVO> fileList = fileDetailService.readFileDetailList(vo.getAtrzFileId());

        AuthorizationDocumentPdfVO approvalPdf = documentPdfService.readAuthorizationDocumentPdf(atrzDocId);

        model.addAttribute("approval", vo);
        model.addAttribute("fileList", fileList);
        model.addAttribute("approvalPdf", approvalPdf);

        return "approval/approvalarchive-detail";
    }

    /**
     * 접근 권한이 없는 문서 접근시 발생하는 예외 처리
     * @param ex
     * @param model
     * @return
     */
    @ExceptionHandler(DocumentAccessDeniedException.class)
    public String handleDocumentAccessDeniedException(DocumentAccessDeniedException ex, Model model) {
        model.addAttribute("message", ex.getMessage());
        return "error/accessDenied";
    }
}
