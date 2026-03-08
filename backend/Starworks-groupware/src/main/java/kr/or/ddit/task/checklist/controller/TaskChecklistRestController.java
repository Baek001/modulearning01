package kr.or.ddit.task.checklist.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import kr.or.ddit.task.checklist.service.TaskChecklistService;
import kr.or.ddit.vo.TaskChecklistVO;
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
 *  2025. 10. 10. 		김주민			  crud 추가
 *
 * </pre>
 */
@RestController
@RequestMapping("/rest/task-checklist")
@RequiredArgsConstructor
public class TaskChecklistRestController {
	
	private final TaskChecklistService service;
	

	/**
	 * 체크리스트 목록 조회
	 * @param taskId
	 * @return
	 */
	@GetMapping("/{taskId}")
	public Map<String, Object> readTaskChecklistList(@PathVariable String taskId) {
		List<TaskChecklistVO> checklists = service.readTaskChecklistList(taskId);
		
		Map<String, Object> result = new HashMap<>();
		result.put("checklists", checklists);
		result.put("count", checklists.size());
		
		return result;
	}
	

	/**
	 * 체크리스트 단건 조회
	 * @param taskId
	 * @param chklistSqn
	 * @return
	 */
	@GetMapping("/{taskId}/{chklistSqn}")
	public Map<String, Object> readTaskChecklist(
		@PathVariable String taskId,
		@PathVariable Integer chklistSqn
	) {
		TaskChecklistVO checklist = service.readTaskChecklist(chklistSqn);
		
		Map<String, Object> result = new HashMap<>();
		result.put("checklist", checklist);
		
		return result;
	}
	
	/**
	 * 체크리스트 생성
	 * @param taskId
	 * @param checklist
	 * @return
	 */
	@PostMapping("/{taskId}")
	public Map<String, Object> createTaskChecklist(
		@PathVariable String taskId,
		@Valid @RequestBody TaskChecklistVO checklist
	) {
		checklist.setTaskId(taskId);
		
		
		
		boolean success = service.createTaskChecklist(checklist);
		
		Map<String, Object> result = new HashMap<>();
		result.put("success", success);
		result.put("checklist", checklist);
		
		return result;
	}
	
	/**
	 * 체크리스트 수정
	 * @param taskId
	 * @param chklistSqn
	 * @param checklist
	 * @return
	 */
	@PutMapping("/{taskId}/{chklistSqn}")
	public Map<String, Object> modifyTaskChecklist(
		@PathVariable String taskId,
		@PathVariable Integer chklistSqn,
		@RequestBody TaskChecklistVO checklist
	) {
		// 기존 데이터 조회
		TaskChecklistVO existingChecklist = service.readTaskChecklist(chklistSqn);
		
		// 전달된 값으로 변경
		if (checklist.getChklistCont() != null) {
			existingChecklist.setChklistCont(checklist.getChklistCont());
		}
		if (checklist.getCompltYn() != null) {
			existingChecklist.setCompltYn(checklist.getCompltYn());
		}
		if (checklist.getTaskPicId() != null) {
			existingChecklist.setTaskPicId(checklist.getTaskPicId());
		}
		
		// 수정 실행
		boolean success = service.modifyTaskChecklist(existingChecklist);
		
		Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("checklist", existingChecklist);
        
        return result;
	}
	
	/**
     * 체크리스트 삭제
     */
    @DeleteMapping("/{taskId}/{chklistSqn}")
    public Map<String, Object> removeTaskChecklist(
            @PathVariable String taskId,
            @PathVariable Integer chklistSqn) {
        
        boolean success = service.removeTaskChecklist(chklistSqn);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        
        return result;
    }
	
}
