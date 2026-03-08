package kr.or.ddit.task.checklist.service;

import java.util.List;

import kr.or.ddit.vo.TaskChecklistVO;

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
 *
 * </pre>
 */
public interface TaskChecklistService {
	
	/**
	 * 체크리스트 추가
	 * @param newTaskChecklist
	 * @return 조회 결과가 없으면 0 으로 false
	 */
	public boolean createTaskChecklist(TaskChecklistVO newTaskChecklist);
	
	/**
	 * 체크리스트 목록 조회
	 * @param taskId
	 * @return 조회 결과 없으면 list.size() == 0
	 */
	public List<TaskChecklistVO> readTaskChecklistList(String taskId);
	
	/**
	 * 체크리스트 단건 조회
	 * @param chklistSqn
	 * @return 조회 결과 없으면 EntityNotFoundException
	 */
	public TaskChecklistVO readTaskChecklist(Integer chklistSqn);
	
	/**
	 * 체크리스트 수정
	 * @param taskChecklist
	 * @return 조회 결과가 없으면 0 으로 false
	 */
	public boolean modifyTaskChecklist(TaskChecklistVO taskChecklist);
	
	/**
	 * 체크리스트 삭제
	 * @param chklistSqn
	 * @return 조회 결과가 없으면 0 으로 false
	 */
	public boolean removeTaskChecklist(Integer chklistSqn);

}
