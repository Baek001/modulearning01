package kr.or.ddit.task.checklist.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.comm.exception.EntityNotFoundException;
import kr.or.ddit.mybatis.mapper.MainTaskMapper;
import kr.or.ddit.mybatis.mapper.ProjectMapper;
import kr.or.ddit.mybatis.mapper.TaskChecklistMapper;
import kr.or.ddit.task.checklist.service.TaskChecklistService;
import kr.or.ddit.vo.MainTaskVO;
import kr.or.ddit.vo.TaskChecklistVO;
import lombok.RequiredArgsConstructor;

/**
 * 
 * @author 김주민
 * @since 2025. 9. 26.
 * @see TaskChecklistServiceImpl
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 26.     	김주민	          최초 생성
 *  2025. 10. 10.		김주민		 진행률 업데이트를 위한 updateTaskAndProjectProgress 추가
 *
 * </pre>
 */
@Service
@RequiredArgsConstructor
public class TaskChecklistServiceImpl implements TaskChecklistService{
	

	private final TaskChecklistMapper mapper;
    private final MainTaskMapper taskMapper;
    private final ProjectMapper projectMapper;
	
	/**
	 * 체크리스트 추가
	 */
	@Override
	@Transactional
	public boolean createTaskChecklist(TaskChecklistVO newTaskChecklist) {
		int rowcnt = mapper.insertTaskChecklist(newTaskChecklist);
		if(rowcnt == 0) {
			return false;
		}
		
		// 업무 및 프로젝트 진행률 자동 업데이트
        updateTaskAndProjectProgress(newTaskChecklist.getTaskId());
		
		return true;
	}

	/**
	 * 체크리스트 목록 조회
	 */
	@Override
	public List<TaskChecklistVO> readTaskChecklistList(String taskId) {
		return mapper.selectTaskChecklistList(taskId);
	}

	/**
	 * 체크리스트 단건 조회
	 */
	@Override
	public TaskChecklistVO readTaskChecklist(Integer chklistSqn) {
		TaskChecklistVO taskChecklist = mapper.selectTaskChecklist(chklistSqn);
		if(taskChecklist == null) {
			throw new EntityNotFoundException(taskChecklist);
		}
		return taskChecklist;
	}

	/**
	 * 체크리스트 수정(완료 여부 변경 시 업무 진행률 자동 업데이트)
	 */
	@Override
	@Transactional
	public boolean modifyTaskChecklist(TaskChecklistVO taskChecklist) {
		int rowcnt = mapper.updateTaskChecklist(taskChecklist);
		if(rowcnt == 0) {
			return false;
		}
		
		// 업무 및 프로젝트 진행률 자동 업데이트
        updateTaskAndProjectProgress(taskChecklist.getTaskId());
		
		return true;
	}

	/**
	 * 체크리스트 삭제
	 */
	@Override
	@Transactional
	public boolean removeTaskChecklist(Integer chklistSqn) {
		// 삭제 전에 taskId 조회
        TaskChecklistVO checklist = mapper.selectTaskChecklist(chklistSqn);
        
        if (checklist == null) {
			return false;
		}
        
        String taskId = checklist.getTaskId();
        
		int rowcnt = mapper.deleteTaskChecklist(chklistSqn);
		if(rowcnt == 0) {
			return false;
		}
		
		// 업무 및 프로젝트 진행률 자동 업데이트
        updateTaskAndProjectProgress(taskId);
		
		return true;
	}
	
	/**
     * 업무 및 프로젝트 진행률 자동 업데이트
     * 
     * 1. 체크리스트 기반으로 업무(MAIN_TASK) 진행률 계산 및 업데이트
     * 2. 모든 업무의 진행률 평균으로 프로젝트(PROJECT) 진행률 업데이트
     */
    private void updateTaskAndProjectProgress(String taskId) {
    	if (taskId == null) {
			return;
		}
        // 체크리스트 업무 조회
        List<TaskChecklistVO> checklists = mapper.selectTaskChecklistList(taskId);
        
        int taskProgress = 0;
        if (checklists != null && !checklists.isEmpty()) { // 체크리스트 목록이 비어있지 않다면
            long completedCount = checklists.stream() // checklists 목록을 스트림으로 변환
                .filter(c -> "Y".equals(c.getCompltYn())) // 필터링 : 완료('Y')된 항목만 걸러냄
                .count(); // 완료된 항목의 총 개수를 셈 (completedCount)
            taskProgress = (int) Math.round((completedCount * 100.0) / checklists.size()); // 진행률 계산
        }
        
        // 업무 진행률 업데이트 (MAIN_TASK.TASK_PRGRS)
        MainTaskVO task = taskMapper.selectMainTask(taskId);
        
        if (task == null) {
			return;
		}
        
        task.setTaskPrgrs(taskProgress);
        taskMapper.updateTaskProgress(task);
        
        // 프로젝트 진행률 업데이트 (PROJECT.BIZ_PRGRS)
        // 해당 업무가 속한 프로젝트의 모든 업무 진행률 평균으로 계산
        projectMapper.updateProjectProgress(task.getBizId());
    }
}
