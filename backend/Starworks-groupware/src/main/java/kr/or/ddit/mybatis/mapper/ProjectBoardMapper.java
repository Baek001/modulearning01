package kr.or.ddit.mybatis.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

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
@Mapper
public interface ProjectBoardMapper {
	
	/**
	 * 조회수 증가
	 * @param bizPstId 프로젝트 게시글 Id
	 * @return 성공한 레코드 수
	 */
	public int updateViewCnt(String bizPstId);
	
	/**
	 * 페이징을 위한 전체 공지사항 개수 조회
	 * @param paramMap : paging + bizId
	 * @return
	 */
	public int selectProjectBoardTotalRecord(Map<String, Object> paramMap);
	
	/**
	 * 프로젝트 게시물 목록 조회(NonPaging).
	 * @return 조회 결과 없으면 list.size() == 0
	 */
	public List<ProjectBoardVO> selectProjectBoardListNonPaging(String bizId);
	
	/**
	 * 프로젝트 게시물 목록 조회(Paging).
	 * @param paramMap : paging + bizId
	 * @return 조회 결과 없으면 list.size() == 0
	 */
	public List<ProjectBoardVO> selectProjectBoardListPaging(Map<String, Object> paramMap);	
	
	/**
	 * 프로젝트 게시물 단건 조회.
	 * @param bizPstId : PROJECT 게시물 ID
	 * @return 조회 결과 없으면 null
	 */
	public ProjectBoardVO selectProjectBoard(String bizPstId);
	
	/**
	 * 프로젝트 게시물 추가.
	 * @param pb : Projevt Board VO 객체
	 * @return 성공 시 1, 실패 시 0
	 */
	public int insertProjectBoard(ProjectBoardVO pb);
	
	/**
	 * 프로젝트 게시물 수정
	 * @param pb : Projevt Board VO 객체
	 * @return 성공 시 1, 실패 시 0
	 */
	public int updateProjectBoard(ProjectBoardVO pb);

	/**
	 * 프로젝트 게시물 삭제 (soft delete)
	 * @param bizPstId :  프로젝트 게시글 Id
	 * @return 성공 시 1, 실패 시 0
	 */
	public int deleteProjectBoard(String bizPstId);
}
