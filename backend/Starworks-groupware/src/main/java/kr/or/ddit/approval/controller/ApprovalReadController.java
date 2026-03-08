package kr.or.ddit.approval.controller;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import kr.or.ddit.approval.document.service.AuthorizationDocumentService;
import kr.or.ddit.approval.line.service.AuthorizationLineService;
import kr.or.ddit.approval.otp.service.OtpService;
import kr.or.ddit.comm.file.service.FileDetailService;
import kr.or.ddit.comm.paging.PaginationInfo;
import kr.or.ddit.comm.paging.SimpleSearch;
import kr.or.ddit.comm.paging.renderer.MazerPaginationRenderer;
import kr.or.ddit.comm.paging.renderer.PaginationRenderer;
import kr.or.ddit.vo.AuthorizationDocumentVO;
import kr.or.ddit.vo.AuthorizationLineVO;
import kr.or.ddit.vo.FileDetailVO;
import lombok.RequiredArgsConstructor;

/**
 * 전자결재 Read 컨트롤러
 */
/**
 *
 * @author 홍현택
 * @since 2025. 10. 2.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 1.     	홍현택	          최초 생성
 *  2025. 10. 5.		홍현택			미열람 -> 미처리 상태 변경 추가
 *  2025. 10.10.		홍현택			페이징 + 검색 기능 추가
 *  2025. 10.21.     	홍현택	      OTP 활성화 + 활성화 여부 확인 로직 추가
 *  2025. 10.24.     	홍현택	      카운트 로직 분리
 *
 * </pre>
 */
@Controller
@RequestMapping("/approval")
@RequiredArgsConstructor
public class ApprovalReadController {

    private final AuthorizationDocumentService service;
    private final FileDetailService fileDetailService;
    private final AuthorizationLineService authorizationLineService;
    private final OtpService otpService; // OTP 서비스 주입

    private static final int screenSize = 5;
    private static final int blockSize = 5;

    @GetMapping("/main")
    public String approvalMain(
            @RequestParam(name = "filter", defaultValue = "all") String filter,
            @RequestParam(name = "page", required = false, defaultValue = "1") int currentPage,
            @ModelAttribute("simpleSearch") SimpleSearch simpleSearch,
            Authentication authentication,
            Model model
    ) {
        String loginId = authentication.getName();

        PaginationInfo<AuthorizationDocumentVO> paging = new PaginationInfo<>(5, 5);
        paging.setCurrentPage(currentPage);
        paging.setSimpleSearch(simpleSearch);

        List<AuthorizationDocumentVO> list = new ArrayList<>();

        if ("inbox".equals(filter)) {
            // 나에게 온 결재
            list = service.readMyInboxCombined(loginId, paging);
        } else if ("drafts".equals(filter)) {
            // 내가 기안한 문서
            list = service.readMyDraftList(loginId, paging);
        } else if ("processed".equals(filter)) {
            // 내가 결재한 문서
            list = service.readMyProcessedList(loginId, paging);
        } else {
            // all (내 문서 전체: 내 기안 내가 결재선 포함)
            list = service.readMyAllCombined(loginId, paging);
        }

        PaginationRenderer renderer = new MazerPaginationRenderer();
        String pagingHTML = renderer.renderPagination(paging, "searchForm");

        model.addAttribute("approvalList", list);
        model.addAttribute("paging", paging);
        model.addAttribute("pagingHTML", pagingHTML);
        model.addAttribute("filter", filter);
        model.addAttribute("userId", loginId);

        return "approval/approval-main";
    }


    // 기존 컨트롤러 일부만 수정
    @GetMapping("/detail/{atrzDocId}")
    public String approvalDetail(
            @PathVariable String atrzDocId,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        // 로그인 사용자 정보
        String loginId = authentication.getName();

        AuthorizationDocumentVO vo = service.readAuthDocument(atrzDocId, loginId);

        // 결재자가 미열람 상태 문서를 상세조회 시, 열람하시겠습니까? 모달창 출력 후 열람 선택 시 미처리 상태로 변경하는 로직임.
        // 현재 로그인 사용자의 미처리 결재 라인 1건 조회 (미열람 A301, 미처리 A302)
        AuthorizationLineVO pendingLine = authorizationLineService.readPendingLineForUser(atrzDocId, loginId);
        if (pendingLine != null && "A301".equals(pendingLine.getAtrzApprStts())) {
            // 미열람 상태이면 미처리로 변경
            authorizationLineService.markAsRead(atrzDocId, pendingLine.getAtrzLineSqn());
            redirectAttributes.addFlashAttribute("message", "문서가 미처리 상태로 변경되었습니다.");
            redirectAttributes.addFlashAttribute("messageType", "success");
            return "redirect:/approval/detail/" + atrzDocId;
        }

        // OTP 활성화 여부 확인
        String secret = otpService.getUserOtpSecret(loginId);
        model.addAttribute("isOtpEnabled", StringUtils.isNotBlank(secret));

        List<FileDetailVO> fileList = fileDetailService.readFileDetailList(vo.getAtrzFileId());
        model.addAttribute("approval", vo);
        model.addAttribute("fileList", fileList);

        return "approval/approval-detail";
    }
}
