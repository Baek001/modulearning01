package kr.or.ddit.task.main.service;

import java.util.List;

import kr.or.ddit.comm.paging.PaginationInfo;
import kr.or.ddit.vo.MainTaskVO;

/**
 *
 * @author 김주민
 * @since 2025. 9. 26.
 * @see MainTaskService
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 26.     	김주민	          최초 생성
 *  2025. 10. 06. 		김주민			주요 업무 목록 조회 추가 (페이징O)
 *  2025. 10. 08. 		김주민			업무 상태 변경 updateTaskStatus, 내 업무 readMyTaskList 추가
 *
 * </pre>
 */
public interface MainTaskService {


	/**
	 * 상태별 업무 리스트 조회
	 * @param userId
	 * @param status
	 * @param paging
	 * @return
	 */
	public List<MainTaskVO> readMyTaskListByStatus(String userId, String status, PaginationInfo<MainTaskVO> paging);

	/**
	 * 내 업무 리스트 조회 (페이징 없음) - 대시보드용
	 */
	public List<MainTaskVO> readMyTaskListNonPaging(String bizUserId);

	/**
	 * 내 업무 리스트 조회
	 * @param bizUserId 담당자 ID (로그인한 사용자ID)
	 * @return 담당 업무 목록
	 */
	public List<MainTaskVO> readMyTaskList(String bizUserId, PaginationInfo<MainTaskVO> paging);

	/**
	 * 업무 상태만 변경
	 * @param taskId
	 * @param taskSttsCd
	 * @return 조회 결과가 없으면 0 으로 false
	 */
	public boolean updateTaskStatus(String taskId, String taskSttsCd);

	/**
     * 주요 업무 조회 (페이징O, 담당자 필터링 지원)
     * @param paging 페이징 정보
     * @param bizId 프로젝트 ID
     * @param bizUserId 담당자 ID (선택사항, null이면 전체 조회)
     * @return 업무 목록
     */
    List<MainTaskVO> readMainTaskList(PaginationInfo<MainTaskVO> paging, String bizId, String bizUserId);

	/**
	 * 주요 업무 추가
	 * @param newMainTask
	 * @return 조회 결과가 없으면 0 으로 false
	 */
	public boolean createMainTask(MainTaskVO newMainTask);

	/**
	 * 주요 업무 목록 조회(페이징X)
	 * @param bizId 프로젝트 ID
	 * @return 조회 결과 없으면 list.size() == 0
	 */
	public List<MainTaskVO> readMainTaskListNonPaging(String bizId);

	/**
	 * 주요 업무 단건 조회
	 * @param taskId
	 * @return 조회 결과 없으면 EntityNotFoundException
	 */
	public MainTaskVO readMainTask(String taskId);

	/**
	 * 주요 업무 수정
	 * @param mainTask
	 * @return 조회 결과가 없으면 0 으로 false
	 */
	public boolean modifyMainTask(MainTaskVO mainTask);

	/**
	 * 주요 업무 삭제
	 * @param taskId
	 * @return 조회 결과가 없으면 0 으로 false
	 */
	public boolean removeMainTask(String taskId);
}
