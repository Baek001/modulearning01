package kr.or.ddit.mybatis.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import kr.or.ddit.vo.TaskChecklistVO;

/**
 * 
 * @author 김주민
 * @since 2025. 9. 25.
 * @see
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
public interface TaskChecklistMapper {
	/**
	 * 주요 업무 체크리스트 등록
	 * @param newTaskChecklist
	 * @return
	 */
	public int insertTaskChecklist(TaskChecklistVO newTaskChecklist);
	
	/**
	 * 주요 업무 체크리스트 목록 조회
	 * @param taskId 업무 ID
	 * @return
	 */
	public List<TaskChecklistVO> selectTaskChecklistList(String taskId);
	
	/**
	 * 주요 업무 체크리스트 단건조회
	 * @param chklistSqn
	 * @return
	 */
	public TaskChecklistVO selectTaskChecklist(Integer chklistSqn);
	
	/**
	 * 주요 업무 체크리스트 수정
	 * @param taskChecklist
	 * @return
	 */
	public int updateTaskChecklist(TaskChecklistVO taskChecklist);
	
	/**
	 * 주요 업무 체크리스트 삭제
	 * @param chklistSqn
	 * @return
	 */
	public int deleteTaskChecklist(Integer chklistSqn);
}
