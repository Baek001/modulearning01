package kr.or.ddit.task.main.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import kr.or.ddit.comm.paging.PaginationInfo;
import kr.or.ddit.comm.paging.renderer.MazerPaginationRenderer;
import kr.or.ddit.comm.paging.renderer.PaginationRenderer;
import kr.or.ddit.mybatis.mapper.FileDetailMapper;
import kr.or.ddit.mybatis.mapper.ProjectMemberMapper;
import kr.or.ddit.security.CustomUserDetails;
import kr.or.ddit.task.main.service.MainTaskService;
import kr.or.ddit.vo.FileDetailVO;
import kr.or.ddit.vo.MainTaskVO;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author 김주민
 * @since 2025. 9. 26.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 26.     	김주민	          최초 생성
 *  2025. 10. 06. 		김주민			  주요 업무 조회 RestController 추가
 *  2025. 10. 07.		김주민			  주요 업무 수정 updateMainTask 추가
 *  2025. 10. 13. 		김주민			  주요 업무 일괄 삭제 deleteMainTasksBatch 추가
 *  2025. 10. 22. 		김주민			  대시보드용 readAllMainTasks 주요업무목록조회 추가
 *  2025. 10. 23.		김주민			  업무 등록 기간 유효성 검사 추가
 *
 * </pre>
 */
@RestController
@RequestMapping("/rest/task")
@RequiredArgsConstructor
public class MainTaskRestController {

	private final MainTaskService service;
	private final FileDetailMapper fileDetailMapper;
	private final ProjectMemberMapper projectMemberMapper;

	/**
	 * 대시보드용 - 전체 업무 목록 조회 (페이징 없음)
	 * @param bizId 프로젝트 ID
	 * @return Map<String, Object>
	 */
	@GetMapping("/list/{bizId}/all")
	public Map<String, Object> readAllMainTasks(@PathVariable String bizId) {
	    Map<String, Object> result = new HashMap<>();

	    List<MainTaskVO> mainTaskList = service.readMainTaskListNonPaging(bizId);

	    result.put("totalRecord", mainTaskList.size());
	    result.put("mainTaskList", mainTaskList);

	    return result;
	}

	/**
	 * 주요 업무 일괄 삭제 - 팀장(B101)만 가능
	 * @param requestBody taskIds 리스트를 담은 Map
	 * @param userDetails 현재 로그인한 사용자 정보
	 * @return Map<String, Object>
	 */
	@PostMapping("/delete/batch")
	public Map<String, Object> deleteMainTasksBatch(
	        @RequestBody Map<String, List<String>> requestBody,
	        @AuthenticationPrincipal CustomUserDetails userDetails) {

	    Map<String, Object> result = new HashMap<>();
	    List<String> taskIds = requestBody.get("taskIds");

	 // 유효성 검사
        if (taskIds == null || taskIds.isEmpty()) {
            result.put("success", false);
            result.put("message", "삭제할 업무를 선택해주세요.");
            return result;
        }

        try {
            // 각 업무에 대한 권한 확인
            String currentUserId = userDetails.getRealUser().getUserId();
            List<String> unauthorizedTasks = new ArrayList<>();

            for (String taskId : taskIds) {
                MainTaskVO task = service.readMainTask(taskId);

                // 권한 확인
                String authCd = projectMemberMapper.getProjectAuthority(
                    task.getBizId(),
                    currentUserId
                );

                if (!"B101".equals(authCd)) {
                    unauthorizedTasks.add(task.getTaskNm());
                }
            }

            // 일괄 삭제 실행
            int deletedCount = 0;
            List<String> failedTasks = new ArrayList<>();

            for (String taskId : taskIds) {
                boolean success = service.removeMainTask(taskId);
                if (success) {
                    deletedCount++;
                } else {
                    failedTasks.add(taskId);
                }
            }

            // 결과 반환
            if (failedTasks.isEmpty()) {
                result.put("success", true);
                result.put("message", deletedCount + "개의 업무가 삭제되었습니다.");
            } else {
                result.put("success", false);
                result.put("message", "일부 업무 삭제에 실패했습니다. (" + deletedCount + "/" + taskIds.size() + " 성공)");
            }

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "업무 삭제 중 오류가 발생했습니다: " + e.getMessage());
        }

        return result;
	}


    /**
     * 주요 업무 삭제 - 팀장(B101)만 가능
     * @param taskId 업무 ID
     * @param userDetails 현재 로그인한 사용자 정보
     * @return Map<String, Object>
     */
    @PutMapping("/delete")
    public Map<String, Object> deleteMainTask(
            @RequestParam String taskId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Map<String, Object> result = new HashMap<>();

        // 업무 조회
        MainTaskVO task = service.readMainTask(taskId);

        // 권한 확인
        String authCd = projectMemberMapper.getProjectAuthority(
            task.getBizId(),
            userDetails.getRealUser().getUserId()
        );

        if (!"B101".equals(authCd)) {
            result.put("success", false);
            result.put("message", "업무 삭제는 프로젝트 담당자만 가능합니다.");
            return result;
        }

        // 삭제 실행
        boolean success = service.removeMainTask(taskId);
        result.put("success", success);

        if(success) {
            result.put("message", "업무가 삭제되었습니다.");
        } else {
            result.put("message", "업무 삭제에 실패했습니다.");
        }

        return result;
    }


    /**
     * 주요 업무 수정 - 팀장(B101)만 가능
     * @param mainTask 업무 정보
     * @param userDetails 현재 로그인한 사용자 정보
     * @return Map<String, Object>
     */
    @PutMapping
    public Map<String, Object> updateMainTask(
            @ModelAttribute MainTaskVO mainTask,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Map<String, Object> result = new HashMap<>();

        // 업무 조회해서 프로젝트 ID 가져오기
        MainTaskVO existingTask = service.readMainTask(mainTask.getTaskId());

        // 권한 확인
        String authCd = projectMemberMapper.getProjectAuthority(
            existingTask.getBizId(),
            userDetails.getRealUser().getUserId()
        );

        LocalDateTime strtDt = mainTask.getStrtTaskDt();
        LocalDateTime endDt = mainTask.getEndTaskDt();

        if (!"B101".equals(authCd)) {
            result.put("success", false);
            result.put("message", "업무 수정은 프로젝트 담당자만 가능합니다.");
            return result;
        }

        //기간 유효성 검사 추가
        if (strtDt == null || endDt == null) {
            result.put("success", false);
            result.put("message", "시작일시와 마감일시를 모두 입력해야 합니다.");
            return result;
        }

        if (strtDt.isAfter(endDt)) {
            result.put("success", false);
            result.put("message", "마감일시는 시작일시보다 빠를 수 없습니다.");
            return result;
        }

        // 수정 실행
        boolean success = service.modifyMainTask(mainTask);
        result.put("success", success);

        if(success) {
            result.put("message", "업무가 수정되었습니다.");
        } else {
            result.put("message", "업무 수정에 실패했습니다.");
        }

        return result;
    }


    /**
     * 업무 상태 변경
     *
     * - 팀장(B101) : 모든 업무의 상태 변경 가능
     * - 팀원(B102) : 본인이 담당한 업무만 상태 변경 가능
     * - 열람자(B103) : 상태 변경 불가
     *
     * @param taskId 업무 ID
     * @param taskSttsCd 업무 상태 코드 (B401:미시작, B402:진행중, B403:보류, B404:완료)
     * @param userDetails 현재 로그인한 사용자 정보
     * @return Map<String, Object>
     */
    @PutMapping("/status")
    public Map<String, Object> updateTaskStatus(
            @RequestParam String taskId,
            @RequestParam String taskSttsCd,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Map<String, Object> result = new HashMap<>();

        // 업무 조회
        MainTaskVO task = service.readMainTask(taskId);
        String currentUserId = userDetails.getRealUser().getUserId();

        // 권한 확인
        String authCd = projectMemberMapper.getProjectAuthority(
            task.getBizId(),
            currentUserId
        );

        // 열람자(B103) 또는 삭제된 멤버(B104)는 불가
        if (authCd == null || "B103".equals(authCd) || "B104".equals(authCd)) {
            result.put("success", false);
            result.put("message", "업무 상태 변경 권한이 없습니다.");
            return result;
        }

        // 참여자(B102)는 본인 담당 업무만 변경 가능
        if ("B102".equals(authCd)) {
            if (!currentUserId.equals(task.getBizUserId())) {
                result.put("success", false);
                result.put("message", "본인이 담당한 업무만 상태를 변경할 수 있습니다.");
                return result;
            }
        }

        // 책임자(B101)는 모든 업무 상태 변경 가능
        // 상태 업데이트
        boolean success = service.updateTaskStatus(taskId, taskSttsCd);
        result.put("success", success);

        if(success) {
            result.put("message", "업무 상태가 변경되었습니다.");
        } else {
            result.put("message", "상태 변경에 실패했습니다.");
        }

        return result;
    }


    /**
     * 주요 업무 등록 - 팀장(B101)만 가능
     * @param newMainTask 등록할 업무 정보
     * @param userDetails 현재 로그인한 사용자 정보
     * @return Map<String, Object>
     */
    @PostMapping
    public Map<String, Object> createMainTask(
            @ModelAttribute MainTaskVO newMainTask,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Map<String, Object> result = new HashMap<>();

        // 1. 권한 확인
        String authCd = projectMemberMapper.getProjectAuthority(
            newMainTask.getBizId(),
            userDetails.getRealUser().getUserId()
        );

        if (!"B101".equals(authCd)) {
            result.put("success", false);
            result.put("message", "업무 등록은 프로젝트 담당자만 가능합니다.");
            return result;
        }

        // + 기간 유효성 검사 추가
        LocalDateTime strtDt = newMainTask.getStrtTaskDt();
        LocalDateTime endDt = newMainTask.getEndTaskDt();

        if(strtDt == null || endDt == null) {
        	result.put("success", false);
        	result.put("message", "시작일시와 마감일시를 모두 입력해야 합니다.");
        	return result;
        }

        if(strtDt.isAfter(endDt)) {
        	result.put("success", false);
        	result.put("message", "마감일시는 시작일시보다 빠를 수 없습니다.");
        	return result;
        }

        // 2. 등록 실행
        boolean success = service.createMainTask(newMainTask);
        result.put("success", success);

        if (success) {
            result.put("message", "업무가 등록되었습니다.");
        } else {
            result.put("message", "업무 등록에 실패했습니다.");
        }

        return result;
    }

    /**
     * 업무 목록 조회 (담당자 필터링 지원)
     * @param bizId 프로젝트 ID
     * @param page 페이지 번호
     * @param bizUserId 담당자 ID (선택사항)
     * @return Map<String, Object>
     */
    @GetMapping("/list/{bizId}")
    public Map<String, Object> readMainTaskList(
            @PathVariable String bizId,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false) String bizUserId) {

        Map<String, Object> result = new HashMap<>();
        PaginationInfo<MainTaskVO> paging = new PaginationInfo<>();
        paging.setCurrentPage(page);

        // 담당자 필터링 포함하여 목록 조회
        List<MainTaskVO> mainTaskList = service.readMainTaskList(paging, bizId, bizUserId);

        PaginationRenderer renderer = new MazerPaginationRenderer();
        String pagingHTML = renderer.renderPagination(paging, "fnMainTaskPaging");

        result.put("totalRecord", paging.getTotalRecord());
        result.put("pagingHTML", pagingHTML);
        result.put("mainTaskList", mainTaskList);

        return result;
    }

    /**
     * 업무 상세 조회
     * @param taskId 업무 ID
     * @return Map<String, Object> - task: 업무 정보, fileList: 첨부파일 목록
     */
    @GetMapping
    public Map<String, Object> readMainTask(@RequestParam String taskId) {
        Map<String, Object> result = new HashMap<>();

        MainTaskVO task = service.readMainTask(taskId);
        result.put("task", task);

        if (task.getTaskFileId() != null) {
            List<FileDetailVO> fileList = fileDetailMapper.selectFileDetailList(task.getTaskFileId());
            result.put("fileList", fileList);
        } else {
            result.put("fileList", new ArrayList<>());
        }

        return result;
    }
}
