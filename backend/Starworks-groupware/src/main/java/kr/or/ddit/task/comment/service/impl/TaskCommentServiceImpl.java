package kr.or.ddit.task.comment.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import kr.or.ddit.comm.exception.EntityNotFoundException;
import kr.or.ddit.mybatis.mapper.MainTaskMapper;
import kr.or.ddit.mybatis.mapper.TaskCommentMapper;
import kr.or.ddit.task.comment.service.TaskCommentService;
import kr.or.ddit.vo.MainTaskVO;
import kr.or.ddit.vo.TaskCommentVO;
import kr.or.ddit.websocket.service.impl.NotificationServiceImpl;
import lombok.RequiredArgsConstructor;

/**
 * 
 * @author 김주민
 * @since 2025. 9. 26.
 * @see TaskCommentServiceImpl
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
@Service
@RequiredArgsConstructor
public class TaskCommentServiceImpl implements TaskCommentService {
	
	
	private final TaskCommentMapper mapper;
	private final NotificationServiceImpl notificationService;
	private final MainTaskMapper mainTaskMapper;

	/**
	 * 업무 코멘트 추가
	 */
	@Override
	public boolean createTaskComment(TaskCommentVO newTaskComment) {
		int rowcnt = mapper.insertTaskComment(newTaskComment);
		if(rowcnt == 0) {
			return false;
		}
		
		MainTaskVO mainTaskId = mainTaskMapper.selectMainTask(newTaskComment.getTaskId());
		
		Map<String, Object> payload = new HashMap<>();
		payload.put("receiverId", mainTaskId.getBizUserId());
		payload.put("senderId", "SYSTEM");
		payload.put("alarmCode", "TASK_02");
		payload.put("pk", mainTaskId.getTaskId());
		
		notificationService.sendNotification(payload);
		
		return true;
	}

	/**
	 * 특정 업무 코멘트 목록 조회
	 */
	@Override
	public List<TaskCommentVO> readTaskCommentList(String taskId) {
		return mapper.selectTaskCommentList(taskId);
	}

	/**
	 * 업무 코멘트 단건 조회
	 */
	@Override
	public TaskCommentVO readTaskComment(Integer taskCommSqn) {
		TaskCommentVO taskComment = mapper.selectTaskComment(taskCommSqn);
		if(taskComment == null) {
			throw new EntityNotFoundException(taskComment);
		}
		return taskComment;
	}

	/**
	 * 업무 코멘트 삭제
	 */
	@Override
	public boolean removeTaskComment(Integer taskCommSqn) {
		int rowcnt = mapper.deleteTaskComment(taskCommSqn);
		if(rowcnt == 0) {
			return false;
		}
		return true;
	}

}
