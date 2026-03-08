package kr.or.ddit.task.comment.service;

import java.util.List;

import kr.or.ddit.vo.TaskCommentVO;

/**
 * 
 * @author 김주민
 * @since 2025. 9. 26.
 * @see TaskCommentService
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
public interface TaskCommentService {
	
	/**
	 * 업무 코멘트 등록
	 * @param newTaskComment
	 * @return 조회 결과가 없으면 0 으로 false
	 */
	public boolean createTaskComment(TaskCommentVO newTaskComment);
	
	/**
	 * 특정 업무에 대한 업무 코멘트 목록 조회
	 * @param taskId 업무 ID
	 * @return 조회 결과 없으면 list.size() == 0
	 */
	public List<TaskCommentVO> readTaskCommentList(String taskId);
	
	/**
	 * 업무 코멘트 단건 조회
	 * @param taskCommSqn
	 * @return 조회 결과 없으면 EntityNotFoundException
	 */
	public TaskCommentVO readTaskComment(Integer taskCommSqn);
	
	
	/**
	 * 업무 코멘트 삭제
	 * @param taskCommSqn
	 * @return 조회 결과가 없으면 0 으로 false
	 */
	public boolean removeTaskComment(Integer taskCommSqn);

}
