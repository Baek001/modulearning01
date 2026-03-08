package kr.or.ddit.project.board.service;

import java.util.List;

import kr.or.ddit.comm.paging.PaginationInfo;
import kr.or.ddit.vo.ProjectBoardVO;

/**
 * 
 * @author 장어진
 * @since 2025. 9. 25.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 25.     	장어진	          최초 생성
 *  2025. 9. 26.     	장어진	          주석 내용 추가
 *  2025.10. 02.     	장어진	          기능 제작을 위한 각종 코드 추가
 *
 * </pre>
 */
public interface ProjectBoardService {
	/**
	 * 조회수 증가
	 * @param bizPstId : 프로젝트 게시물 ID
	 * @return 성공 시 true, 실패 시 false
	 */
	public boolean modifyViewCnt(String bizPstId);

	/**
	 * 페이징을 위한 전체 공지사항 개수 조회
	 * @param paging
	 * @return
	 */
	public int readProjectBoardTotalRecord(PaginationInfo<ProjectBoardVO> paging, String bizId);	
	
	/**
	 * 프로젝트 게시물 목록 조회(NonPaging)
	 * @param bizId : 프로젝트 ID 
	 * @return 조회 결과 없으면 list.size() == 0
	 */
	public List<ProjectBoardVO> readProjectBoardListNonPaging(String bizId);
	
	/**
	 * 프로젝트 게시물 목록 조회 (Paging)
	 * @param paging 
	 * @param bizId : 프로젝트 ID
	 * @return 조회 결과 없으면 list.size() == 0
	 */
	public List<ProjectBoardVO> readProjectBoardListPaging(PaginationInfo<ProjectBoardVO> paging, String bizId);
	
	/**
	 * 프로젝트 게시물 단건 조회
	 * @param bizPstId : PROJECT 게시물 ID
	 * @return 조회 결과 없으면 EntityNotFoundException
	 */
	public ProjectBoardVO readProjectBoard(String bizPstId);
	
	/**
	 * 프로젝트 게시물 생성
	 * @param pb : Projevt Board VO 객체
	 * @return 성공 시 true, 실패 시 false
	 */
	public boolean createProjectBoard(ProjectBoardVO pb);
	
	/**
	 * 프로젝트 게시물 수정
	 * @param pb : Projevt Board VO 객체
	 * @return 성공 시 true, 실패 시 false
	 */
	public boolean modifyProjectBoard(ProjectBoardVO pb);
	
	/**
	 * 프로젝트 게시물 삭제 (soft delete)
	 * @param bizPstId : PROJECT 게시물 ID
	 * @return 성공 시 true, 실패 시 false
	 */
	public boolean removeProjectBoard(String bizPstId);
}
