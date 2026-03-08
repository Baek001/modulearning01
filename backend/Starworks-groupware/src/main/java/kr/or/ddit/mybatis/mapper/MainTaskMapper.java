package kr.or.ddit.mybatis.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import kr.or.ddit.comm.paging.PaginationInfo;
import kr.or.ddit.vo.MainTaskVO;

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
 *  2025. 10. 06.		김주민			  주요 업무 조회(페이징O) selectMatinTask 추가
 *  2025. 10. 08. 		김주민			  updateTaskStatus 추가
 *  2025. 10. 10. 		김주민			  updateTaskProgress 추가(진행률 업데이트를 위한),selectMyTaskList 추가
 *  2025. 10. 22.		김주민			  내 업무 리스트 조회 페이징처리 추가
 *
 * </pre>
 */
@Mapper
public interface MainTaskMapper {
	/**
	 * 프로젝트 업무 전체 레코드 수 조회 (담당자 필터링 지원)
	 * @param bizId 프로젝트 ID
	 * @param bizUserId 담당자 ID (선택사항)
	 * @return 전체 레코드 수
	 */
	int selectTotalRecord(@Param("bizId") String bizId, @Param("bizUserId") String bizUserId);

	public int selectMyTaskTotalRecord(String bizUserId); //내 업무 총 개수 조회

	public int selectMyTaskTotalRecordByStatus(@Param("userId") String userId, @Param("status") String status);

	/**
	 * 상태별 업무 조회.
	 */
	public List<MainTaskVO> selectMyTaskListByStatus(@Param("userId") String userId,
	                                          @Param("status") String status,
	                                          @Param("paging") PaginationInfo<MainTaskVO> paging);

	/**
	 * 내 업무 리스트 조회 (페이징 없음) - 대시보드용
	 */
	public List<MainTaskVO> selectMyTaskListNonPaging(String bizUserId);

	/**
	 * 특정 사용자가 담당하는 업무 목록 조회
	 * @param bizUserId 담당자 ID
	 * @return 담당 업무 목록
	 */
	public List<MainTaskVO> selectMyTaskList(@Param("bizUserId") String bizUserId, @Param("paging") PaginationInfo<MainTaskVO> paging);

	/**
	 * 업무 진행률만 업데이트
	 * @param task
	 * @return
	 */
	public int updateTaskProgress(MainTaskVO task);

	/**
	 * 업무 상태만 업데이트
	 */
	public int updateTaskStatus(@Param("taskId") String taskId, @Param("taskSttsCd") String taskSttsCd);

	/**
	 * 프로젝트 업무 목록 조회 (페이징O, 담당자 필터링 지원)
	 * @param paging 페이징 정보
	 * @param bizId 프로젝트 ID
	 * @param bizUserId 담당자 ID (선택사항)
	 * @return 업무 목록
	 */
	List<MainTaskVO> selectMainTaskList(
		@Param("paging") PaginationInfo<MainTaskVO> paging,
		@Param("bizId") String bizId,
		@Param("bizUserId") String bizUserId
	);

	/**
	 * 주요 업무 등록
	 * @param newMainTask
	 * @return
	 */
	public int insertMainTask(MainTaskVO newMainTask);

	/**
	 * 주요 업무 조회
	 * @param bizId 프로젝트 ID
	 * @return 해당 프로젝트의 업무 List
	 */
	public List<MainTaskVO> selectMainTaskListNonPaging(String bizId);

	/**
	 * 주요 업무 단건 조회
	 * @param taskId
	 * @return
	 */
	public MainTaskVO selectMainTask(String taskId);

	/**
	 * 주요 업무 수정
	 * @param mainTask
	 * @return
	 */
	public int updateMainTask(MainTaskVO mainTask);

	/**
	 * 주요 업무 삭제
	 * @param mainTask
	 * @return
	 */
	public int deleteMainTask(String taskId);
}
