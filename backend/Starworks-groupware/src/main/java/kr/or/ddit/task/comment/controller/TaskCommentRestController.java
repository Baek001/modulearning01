package kr.or.ddit.task.comment.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kr.or.ddit.comm.validate.InsertGroupNotDefault;
import kr.or.ddit.security.CustomUserDetails;
import kr.or.ddit.task.comment.service.TaskCommentService;
import kr.or.ddit.vo.TaskCommentVO;
import kr.or.ddit.vo.UsersVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 *  2025. 10. 09. 		김주민			  코멘트 crud 추가
 *
 * </pre>
 */
@Slf4j
@RestController
@RequestMapping("/rest/task-comment")
@RequiredArgsConstructor
public class TaskCommentRestController {
	
	private final TaskCommentService service;
	
	/**
	 * 특정 업무에 대한 업무 코멘트 목록 조회 RestController
	 * @param taskId
	 * @return Map<String, Object> - commentList : 코멘트 목록, totalCount: 총 개수
	 */
	@GetMapping("/{taskId}")
	public Map<String, Object> readTaskCommentList(@PathVariable String taskId) {
		log.debug("코멘트 목록 조회 - taskId: {}", taskId);
		
		List<TaskCommentVO> commentList = service.readTaskCommentList(taskId);
		
		Map<String, Object> result = new HashMap<>();
		result.put("commentList", commentList);
		result.put("totalCount", commentList.size());
		
		return result;
	}
	
	/**
	 * 업무 코멘트 단건 조회 RestController
	 * @param taskCommSqn 코멘트 sqn
	 * @return 조회 결과 없으면 EntityNotFoundException
	 */
	@GetMapping("/{taskId}/{taskCommSqn}")
	public TaskCommentVO readTaskComment(
		@PathVariable String taskId,
		@PathVariable Integer taskCommSqn
	) {
		return service.readTaskComment(taskCommSqn);
	}
	
	/**
     * 업무 코멘트 등록
     * @param taskId 업무 ID
     * @param commentVO 코멘트 정보
     * @param authentication 인증 정보
     * @return Map<String, Object> - success: 성공 여부, comment: 등록된 코멘트 정보
     */
    @PostMapping("/{taskId}")
    public Map<String, Object> createTaskComment(
            @PathVariable String taskId,
            @Validated(InsertGroupNotDefault.class) @RequestBody TaskCommentVO commentVO,
            Authentication authentication) {
        
        log.debug("코멘트 등록 - taskId: {}, contents: {}", taskId, commentVO.getContents());
        
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        UsersVO realUser = userDetails.getRealUser();
        
        // 코멘트 정보 설정
        commentVO.setTaskId(taskId);
        commentVO.setCrtUserId(realUser.getUserId());
        
        boolean success = service.createTaskComment(commentVO);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        
        if (success) {
            // 등록된 코멘트 정보 조회 (사용자명 포함)
            TaskCommentVO newComment = service.readTaskComment(commentVO.getTaskCommSqn());
            result.put("comment", newComment);
            log.debug("코멘트 등록 성공 - taskCommSqn: {}", newComment.getTaskCommSqn());
        } else {
            log.warn("코멘트 등록 실패");
        }
        
        return result;
    }
    
    /**
     * 업무 코멘트 삭제
     * @param taskId 업무 ID
     * @param taskCommSqn 코멘트 일련번호
     * @return Map<String, Object> - success: 성공 여부
     */
    @DeleteMapping("/{taskId}/{taskCommSqn}")
    public Map<String, Object> removeTaskComment(
            @PathVariable String taskId,
            @PathVariable Integer taskCommSqn) {
        
        log.debug("코멘트 삭제 - taskId: {}, taskCommSqn: {}", taskId, taskCommSqn);
        
        boolean success = service.removeTaskComment(taskCommSqn);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        
        if (success) {
            log.debug("코멘트 삭제 성공");
        } else {
            log.warn("코멘트 삭제 실패");
        }
        
        return result;
    }

}
