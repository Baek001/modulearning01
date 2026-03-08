package kr.or.ddit.email.controller;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import kr.or.ddit.comm.file.service.FileDetailService;
import kr.or.ddit.comm.paging.PaginationInfo;
import kr.or.ddit.comm.paging.SimpleSearch;
import kr.or.ddit.comm.paging.renderer.MazerPaginationRenderer;
import kr.or.ddit.comm.paging.renderer.PaginationRenderer;
import kr.or.ddit.email.box.service.EmailBoxService;
import kr.or.ddit.email.content.service.EmailContentService; // EmailContentService import 추가
import kr.or.ddit.vo.EmailContentVO;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author 홍현택
 * @since 2025. 10. 10.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 10.     	홍현택	          최초 생성
 *  2025. 10. 13. 		홍현택			이메일 상세 조회
 *  2025. 10. 13. 		홍현택			첨부파일 기능 추가
 *  2025. 10. 14. 		홍현택			중요 메일 상태 토글 추가
 *  2025. 10. 15. 		홍현택			메일함 별 총 이메일 갯수 카운트 getMailboxCounts
 *
 * </pre>
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/mail")
public class EmailReadController {

    private final EmailBoxService emailBoxService;
    private final EmailContentService emailContentService; // emailContentService 주입
    private final FileDetailService fileDetailService; // FileDetailService 주입

    /**
     * 메일 목록 페이지
     * @return 메일 목록 뷰 경로
     */
    @GetMapping("/list")
    public String mailListPage() {



        return "mail/mail-list";
    }

    /**
     * 특정 사용자의 메일함 유형에 따른 이메일 목록
     * @param principal 현재 로그인한 사용자 정보
     * @param mailboxTypeCd 메일함 유형 코드 (예: G101 for Inbox)
     * @param currentPage 현재 페이지 번호
     * @return 이메일 목록 (EmailContentVO 형태로 반환) 및 페이징 정보
     */
    @GetMapping("/listData/{mailboxTypeCd}")
    @ResponseBody
    public Map<String, Object> searchEmailList(
    		Principal principal,
    		@PathVariable String mailboxTypeCd,
    		@RequestParam(name = "page", required = false, defaultValue = "1") int currentPage,
    		@ModelAttribute("search") SimpleSearch search
    	) {
        String userId = principal.getName();

        PaginationInfo<EmailContentVO> paging = new PaginationInfo<>(10, 5);
        paging.setCurrentPage(currentPage);

        // 검색 조건 설정
        paging.setSimpleSearch(search);

        int totalRecord = emailBoxService.readEmailListCount(userId, mailboxTypeCd, paging);
        paging.setTotalRecord(totalRecord);

        List<EmailContentVO> emailList = emailBoxService.searchEmailList(userId, mailboxTypeCd, paging);

        PaginationRenderer renderer = new MazerPaginationRenderer();
        String paginationHTML = renderer.renderPagination(paging, "listEmail");

        Map<String, Object> result = new HashMap<>();
        result.put("emailList", emailList);
        result.put("paginationHTML", paginationHTML);
        result.put("paging", paging);
        result.put("currentUserId", userId);

        return result;
    }

    /**
     * 이메일 상세 조회
     * @param principal 현재 로그인한 사용자 정보
     * @param emailContId 조회할 이메일의 ID
     * @param model View에 전달할 데이터를 담는 객체
     * @return 메일 상세 페이지 뷰 경로
     */
    @GetMapping("/detail/{emailContId}")
    public String mailDetail(
    		Principal principal,
    		@PathVariable String emailContId,
    		org.springframework.ui.Model model
    	) {
        String userId = principal.getName();
        EmailContentVO email = emailContentService.readEmailDetail(emailContId, userId);
        model.addAttribute("email", email);
        return "mail/mail-detail";
    }

    /**
	 * 중요 메일 상태를 토글(추가/제거)합니다.
	 * @param principal 현재 로그인한 사용자 정보
	 * @param emailContId 토글할 이메일의 ID
	 * @return 작업 결과 (성공 여부, 새로운 중요 상태)를 담은 Map 객체
	 */
	@PostMapping("/toggle-importance/{emailContId}")
	@ResponseBody
	public Map<String, Object> toggleImportance(
			Principal principal,
			@PathVariable String emailContId
		) {
		String userId = principal.getName();
		boolean isImportant = emailContentService.toggleImportance(userId, emailContId);

		Map<String, Object> result = new HashMap<>();
		result.put("status", "success");
		result.put("isImportant", isImportant);

		return result;
	}

    /**
     * 각 메일함의 이메일 개수를 반환합니다.
     * @param principal 현재 로그인한 사용자 정보
     * @return 각 메일함의 이메일 개수를 담은 Map 객체 (JSON 형식)
     */
    @GetMapping("/counts")
    @ResponseBody
    public Map<String, Integer> getMailboxCounts(Principal principal) {
        String userId = principal.getName();
        Map<String, Integer> counts = new HashMap<>();

        // 각 메일함 유형별 개수 조회
        // PaginationInfo는 totalRecord를 얻기 위해 사용
        counts.put("inboxCount", emailBoxService.readEmailListCount(userId, "G101", new PaginationInfo<>(1,1)));
        counts.put("sentCount", emailBoxService.readEmailListCount(userId, "G102", new PaginationInfo<>(1,1)));
        counts.put("draftsCount", emailBoxService.readEmailListCount(userId, "G103", new PaginationInfo<>(1,1)));
        counts.put("importantCount", emailBoxService.readEmailListCount(userId, "G104", new PaginationInfo<>(1,1)));
        counts.put("trashCount", emailBoxService.readEmailListCount(userId, "G105", new PaginationInfo<>(1,1)));
        // 받은 편지함의 읽지 않은 메일 개수 조회
        counts.put("unreadInboxCount", emailBoxService.readUnreadEmailCount(userId, "G101"));

        return counts;
    }

}
