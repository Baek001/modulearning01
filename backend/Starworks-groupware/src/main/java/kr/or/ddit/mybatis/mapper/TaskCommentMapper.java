package kr.or.ddit.mybatis.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import kr.or.ddit.vo.TaskCommentVO;

/**
 * 
 * @author 김주민
 * @since 2025. 9. 25.
 * @see TaskCommentMapper
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 25.     	김주민	          최초 생성
 *
 * </pre>
 */
@Mapper
public interface TaskCommentMapper {
	/**
	 * 업무 코멘트 등록
	 * @param newTaskComment
	 * @return
	 */
	public int insertTaskComment(TaskCommentVO newTaskComment);
	
	/**
	 * 업무 코멘트 목록
	 * @param taskId 업무 ID
	 * @return
	 */
	public List<TaskCommentVO> selectTaskCommentList(String taskId);
	
	/**
	 * 업무 코멘트 단건
	 * @param taskCommSqn
	 * @return
	 */
	public TaskCommentVO selectTaskComment(Integer taskCommSqn);
	
	/**
	 * 업무 코멘트 삭제
	 * @param taskCommSqn
	 * @return
	 */
	public int deleteTaskComment(Integer taskCommSqn);
}
