package kr.or.ddit.project.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import kr.or.ddit.comm.file.service.FileDetailService;
import kr.or.ddit.comm.paging.PaginationInfo;
import kr.or.ddit.comm.paging.SimpleSearch;
import kr.or.ddit.comm.paging.renderer.MazerPaginationRenderer;
import kr.or.ddit.comm.paging.renderer.PaginationRenderer;
import kr.or.ddit.mybatis.mapper.ProjectMemberMapper;
import kr.or.ddit.project.member.service.projectMemberService;
import kr.or.ddit.project.mngt.service.projectService;
import kr.or.ddit.security.CustomUserDetails;
import kr.or.ddit.task.main.service.MainTaskService;
import kr.or.ddit.vo.FileDetailVO;
import kr.or.ddit.vo.MainTaskVO;
import kr.or.ddit.vo.ProjectMemberVO;
import kr.or.ddit.vo.ProjectVO;

/**
 *
 * @author 김주민
 * @since 2025. 9. 29.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 29.     	김주민	          최초 생성
 *  2025. 9. 29.    	김주민  			projectMyProject, paging 추가
 *  2025. 10. 07. 		김주민			프로젝트 단건 조회 권한 체크 추가
 *  2025. 10. 16. 		김주민			내 프로젝트 컨트롤러 분리
 *
 * </pre>
 */
@Controller
@RequestMapping("/projects")
public class ProjectController {
	@Autowired
	private projectService service;

	@Autowired
	private projectMemberService memberService;

	@Autowired
	private FileDetailService fileDetailService;

	@Autowired
    private ProjectMemberMapper projectMemberMapper;

	@Autowired
    private MainTaskService mainTaskService;

    /**
     * 전사 프로젝트 목록 페이지로 이동합니다.
     * @param model
     * @return "project/project-list"
     */
    @GetMapping("/all")
    public String projectList(
    		@ModelAttribute ProjectVO searchVO,
    		@RequestParam(required = false, defaultValue = "1") int page,
    		@ModelAttribute("search") SimpleSearch search,
    		@AuthenticationPrincipal CustomUserDetails userDetails,
    		Model model
    	) {

    	PaginationInfo<ProjectVO> paging = new PaginationInfo<>(8,5);
    	paging.setCurrentPage(page);
    	paging.setSimpleSearch(search);
    	paging.setDetailSearch(searchVO);

    	List<ProjectVO> projectList = service.readProjectList(paging);

    	// 로그인한 사용자의 각 프로젝트 접근 권한 체크
        if (userDetails != null) {
            String userId = userDetails.getUsername();
            for (ProjectVO project : projectList) {
                boolean hasAccess = checkUserAccess(project.getBizId(), userId, project);
                project.setHasAccess(hasAccess);
            }
        } else {
            // 비로그인 사용자는 모두 접근 불가
            for (ProjectVO project : projectList) {
                project.setHasAccess(false);
            }
        }

    	PaginationRenderer renderer = new MazerPaginationRenderer();
    	String pagingHTML = renderer.renderPagination(paging, "fnPaging");

        model.addAttribute("projectList", projectList);
        model.addAttribute("pagingHTML", pagingHTML);
        model.addAttribute("currentSubMenu", "list");

        return "project/project-list";
    }

    /**
     * 내 프로젝트 페이지로 이동합니다.
     * @param model
     * @return "project/project-myproject"
     */
    @GetMapping("/main")
    public String projectMyProject(Model model,
    		@RequestParam(required = false, defaultValue = "1") int projectPage,
    		@RequestParam(required = false, defaultValue = "1") int taskPage,
    		@AuthenticationPrincipal CustomUserDetails userDetails) {
    	// 인증 정보가 null인 경우
        if (userDetails == null) {
            return "redirect:/login";
        }

        // CustomUserDetails 객체에서 userId를 추출
        String userId = userDetails.getUsername();

        PaginationInfo<ProjectVO> projectPaging = new PaginationInfo<>(10,5);
        projectPaging.setCurrentPage(projectPage);

        // 프로젝트 목록 조회
        List<ProjectVO> projectList = service.readMyProjectList(userId, projectPaging);

       // 각 프로젝트에 대해 멤버 테이블을 조회하여 ProjectVO 객체에 참여자 정보를 추가
        for (ProjectVO project : projectList) {
            List<ProjectMemberVO> members = memberService.readProjectMemberList(project.getBizId());
            project.setMembers(members);
        }

        // 업무 목록 페이징
        PaginationInfo<MainTaskVO> taskPaging = new PaginationInfo<>(6,5);
        taskPaging.setCurrentPage(taskPage);

        // 내 업무 목록 조회(페이징 적용)
        List<MainTaskVO> myTaskList = mainTaskService.readMyTaskList(userId, taskPaging);

        // 전체 업무 목록 조회(페이징 없음) - 차트/카드 계산용
        List<MainTaskVO> allMyTaskList = mainTaskService.readMyTaskListNonPaging(userId);

        PaginationRenderer renderer = new MazerPaginationRenderer();
    	String pagingHTML = renderer.renderPagination(projectPaging, "fnProjectPaging");
        model.addAttribute("allMyTaskList", allMyTaskList);
    	String taskPagingHTML = renderer.renderPagination(taskPaging, "fnTaskPaging");

        model.addAttribute("projectList", projectList);
        model.addAttribute("myTaskList", myTaskList);

        model.addAttribute("pagingHTML", pagingHTML);
        model.addAttribute("taskPagingHTML", taskPagingHTML);
        return "project/project-myproject";
    }


    /**
     * 진행 중인 프로젝트
     * @param model
     * @param page
     * @param userDetails
     * @return "project/project-my-progress"
     */
    @GetMapping("/my/progress")
    public String myInProgressProjectList(Model model,
            @RequestParam(required = false, defaultValue = "1") int page,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) {
            return "redirect:/login";
        }

        String userId = userDetails.getUsername();
        PaginationInfo<ProjectVO> paging = new PaginationInfo<>(6,5);
        paging.setCurrentPage(page);

        // 진행 중 프로젝트 목록 조회
        List<ProjectVO> projectList = service.readMyProjectList(userId, paging);

        // 각 프로젝트에 대해 멤버 테이블을 조회하여 ProjectVO 객체에 참여자 정보를 추가
        for (ProjectVO project : projectList) {
            List<ProjectMemberVO> members = memberService.readProjectMemberList(project.getBizId());
            project.setMembers(members);
        }

        PaginationRenderer renderer = new MazerPaginationRenderer();
        String pagingHTML = renderer.renderPagination(paging, "fnPaging");

        model.addAttribute("projectList", projectList);
        model.addAttribute("pagingHTML", pagingHTML);

        return "project/project-my-progress";
    }

    /**
     * 진행한 프로젝트 (완료 B304) 페이지로 이동 및 데이터 조회
     * @param model
     * @param page
     * @param userDetails
     * @return "project/project-my-completed"
     */
    @GetMapping("/my/completed")
    public String myCompletedProjectList(Model model,
            @RequestParam(required = false, defaultValue = "1") int page,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) {
            return "redirect:/login";
        }

        String userId = userDetails.getUsername();
        PaginationInfo<ProjectVO> paging = new PaginationInfo<>(6,5);
        paging.setCurrentPage(page);

        // 완료 프로젝트 목록 조회 (B304)
        List<ProjectVO> projectList = service.readMyCompletedProjectList(userId, paging);

        // 각 프로젝트에 대해 멤버 테이블을 조회하여 ProjectVO 객체에 참여자 정보를 추가
        for (ProjectVO project : projectList) {
            List<ProjectMemberVO> members = memberService.readProjectMemberList(project.getBizId());
            project.setMembers(members);
        }

        PaginationRenderer renderer = new MazerPaginationRenderer();
        String pagingHTML = renderer.renderPagination(paging, "fnPaging");

        model.addAttribute("projectList", projectList);
        model.addAttribute("pagingHTML", pagingHTML);

        return "project/project-my-completed";
    }

    /**
   	 * 사용자의 프로젝트 접근 권한 확인
   	 * @param bizId 프로젝트 ID
   	 * @param userId 사용자 ID
   	 * @param project 프로젝트 정보
   	 * @return 접근 가능 여부
   	 */
   	private boolean checkUserAccess(String bizId, String userId, ProjectVO project) {
   		// 책임자인지 확인
   		if (project.getBizPicId().equals(userId)) {
   			return true;
   		}

   		// 프로젝트 멤버(참여자/열람자)인지 확인
   		ProjectMemberVO searchMember = new ProjectMemberVO();
   		searchMember.setBizId(bizId);
   		searchMember.setBizUserId(userId);

   		ProjectMemberVO member = projectMemberMapper.selectProjectMember(searchMember);
   		if (member != null) {
   			// B102: 참여자, B103: 열람자
   			return "B102".equals(member.getBizAuthCd()) || "B103".equals(member.getBizAuthCd());
   		}

   		return false;
   	}

    /**
     * 프로젝트 상세 페이지로 이동합니다. (권한 체크 추가)
     * @param model
     * @return "project/project-detail"
     */
    @GetMapping("/{bizId}")
    public String projectDetail(
    		Model model,
    		@PathVariable String bizId,
    		@AuthenticationPrincipal CustomUserDetails userDetails,
    		RedirectAttributes redirectAttributes
    	) {

    	// 프로젝트 정보 조회
    	ProjectVO project = service.readProject(bizId);
    	String userId = userDetails.getUsername();

    	// 승인 대기 상태 체크
        if ("승인 대기".equals(project.getBizSttsCd())) {
            redirectAttributes.addFlashAttribute("icon", "warning");
            redirectAttributes.addFlashAttribute("toastMessage", "승인 대기 중인 프로젝트는 접근할 수 없습니다.");
            return "redirect:/projects/all";
        }

        // 권한 체크
    	boolean hasAccess = checkUserAccess(bizId, userId, project);
    	if (!hasAccess) {
    		// SweetAlert2 Toast를 위한 Flash Attribute 설정
    		redirectAttributes.addFlashAttribute("icon", "error");
    		redirectAttributes.addFlashAttribute("toastMessage", "이 프로젝트에 대한 접근 권한이 없습니다.");

    		return "redirect:/projects/all";
    	}

    	// === 권한이 있는 경우 상세 정보 조회 및 모델 추가 ===
    	// 프로젝트 멤버 조회
    	List<ProjectMemberVO> memberList = memberService.readProjectMemberList(bizId);

    	// 첨부 파일 목록 조회
    	List<FileDetailVO> fileList = fileDetailService.readFileDetailList(project.getBizFileId());

    	// 현재 사용자 권한 코드 계산
    	String currentUserAuthCode = "";

    	// 프로젝트 책임자인지 확인
    	if (project.getBizPicId().equals(userId)) {
    		currentUserAuthCode = "B101";
    	} else {
    		// 멤버 리스트에서 권한 찾기
    		for (ProjectMemberVO member : memberList) {
    			if (member.getBizUserId().equals(userId)) {
    				currentUserAuthCode = member.getBizAuthCd();
    				break;
    			}
    		}
    	}

    	model.addAttribute("fileList", fileList);
    	model.addAttribute("project", project);
    	model.addAttribute("memberList", memberList);
    	model.addAttribute("currentUserId", userId);
    	model.addAttribute("currentUserAuthCode", currentUserAuthCode);

    	return "project/project-detail";
    }

    /**
     * 프로젝트 보관함 페이지로 이동합니다.
     * @param model
     * @return "project/project-archive"
     */
    @GetMapping("/archive")
    public String projectArchive(
    		@ModelAttribute ProjectVO searchVO,
    		@RequestParam(required = false, defaultValue = "1") int page,
    		@ModelAttribute("search") SimpleSearch search,
    		Model model
    	) {
    	PaginationInfo<ProjectVO> paging = new PaginationInfo<>(8,5);
    	paging.setCurrentPage(page);
    	paging.setSimpleSearch(search);
    	paging.setDetailSearch(searchVO);

    	List<ProjectVO> archivedProjectList = service.readArchivedProjectList(paging);

    	PaginationRenderer renderer = new MazerPaginationRenderer();
    	String pagingHTML = renderer.renderPagination(paging, "fnPaging");

        model.addAttribute("archivedProjectList", archivedProjectList);
        model.addAttribute("pagingHTML", pagingHTML);

        return "project/project-archive";
    }
}
