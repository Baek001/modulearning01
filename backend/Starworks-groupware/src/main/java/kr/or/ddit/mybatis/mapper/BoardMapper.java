package kr.or.ddit.mybatis.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import kr.or.ddit.comm.paging.PaginationInfo;
import kr.or.ddit.vo.BoardVO;

/**
 *
 * @author 홍현택
 * @since 2025. 9. 25.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 25.     	홍현택	        공지사항게시판 crud 메서드 생성
 *  2025. 9. 25.		임가영			insert, update 매개변수 BoardVO pstId => BoardVO board 로 이름 변경, 공지사항과 자유게시판 조회 분리 / insert 제외 모든 테스트 끝남 / public 접근제한자 추가
 *  2025. 9. 27.		임가영			updateViewCnt(조회수 증가) 메소드 추가
 *  2025.10. 05. 		홍현택			selectNoticeTotalRecord  메서드 추가
 *  2025.10. 17.		홍현택			selectPopularCommunityList(인기게시글, 조회수순), selectPopularCommunityTotalRecord(페이징용) 추가
 *  2025.10. 24.		홍현택			selectCategoryCounts(카운트) 추가
 * </pre>
 */
@Mapper
public interface BoardMapper {

	//////////////////////////////// 공지사항 ////////////////////////////////
	/**
	 * 페이징을 위한 전체 공지사항 목록 출력 메서드
	 * @param paging
	 * @return
	 */
	public int selectNoticeTotalRecord(PaginationInfo<BoardVO> paging);

	/**
	 * 공지사항 목록을 다건 출력하는 페이징 처리 메서드
	 * @param paging
	 * @return
	 */
	public List<BoardVO> selectNoticeList(PaginationInfo<BoardVO> paging);

	/** Nonpaging 처리
	 * @return
	 */
	public List<BoardVO> selectNoticeListNonPaging();

	/**
	 * 관리자 대시보드 공지사항
	 * @return
	 */
	public List<BoardVO> selectNotices();

	/**
	 * 커뮤니티 카테고리별 게시물 수 조회
	 * @return 카테고리 코드와 게시물 수를 담은 Map 리스트
	 */
	public List<Map<String, Object>> selectCategoryCounts();

	//////////////////////////////// 자유게시판 ////////////////////////////////
	/**
	 * 커뮤니티 게시물 개수 조회
	 * @return
	 */
	public int selectCommunityTotalRecord(Map<String, Object> paramMap);

	/**
	 * 사내 커뮤니티 전체 조회. (페이징 O)
	 * @param paging
	 * @return
	 */
	public List<BoardVO> selectCommunityList(Map<String, Object> paramMap);

	/**
	 * 인기 커뮤니티 게시물 목록 조회 (조회수 기준, 페이징 O)
	 * @param paramMap
	 * @return
	 */
	public List<BoardVO> selectPopularCommunityList(Map<String, Object> paramMap);

	/**
	 * 인기 커뮤니티 게시물 개수 조회
	 * @param paramMap
	 * @return
	 */
	public int selectPopularCommunityTotalRecord(Map<String, Object> paramMap);

	/**
	 * 사내 커뮤니티 전체 조회. (페이징 X)
	 * bbsCtgrCd 값이 없으면 전체 조회, 있으면 카테고리별 조회
	 * F102	동호회
	 * F103 경조사
	 * F104	사내활동
	 * F105	건의사항
	 * F106 기타
	 * @return 조회 결과 없으면 list.size() == 0.
	 */
	public List<BoardVO> selectCommunityListNonPaging(String bbsCtgrCd);

	////////////////////////////////////////////////////////////////////////
	/**
	 * 조회수 증가
	 * @param pstId 게시글 Id
	 * @return 성공한 레코드 수
	 */
	public int updateViewCnt(String pstId);
	/**
	 * 한건의 게시물을 출력하는 메서드
	 * @param pstID
	 * @return
	 */
	public BoardVO selectBoard(String pstId);

	/**
	 * 한건의 게시물을 추가(작성)하는 메서드
	 * @param pstID
	 * @return 성공한 레코드 수
	 */
	public int insertBoard(BoardVO board);

	/**
	 * 한건의 게시물을 수정하는 메서드
	 * @param pstID
	 * @return 성공한 레코드 수
	 */
	public int updateBoard(BoardVO board);

	/**
	 * 한건의 게시물을 삭제 하는 메서드
	 * @param pstId
	 * @return 성공한 레코드 수
	 */
	public int deleteBoard(String pstId);

}
