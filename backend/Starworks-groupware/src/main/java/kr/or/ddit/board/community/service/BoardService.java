package kr.or.ddit.board.community.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

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
 *  2025. 9. 25.     	홍현택	        공지사항 crud 메서드 생성
 *  2025. 9. 25.		임가영			공지사항과 자유게시판 조회 분리 / 자유게시판 crud 메서드 생성 / public 접근제한자 추가
 *	2025. 9. 27.		임가영			updateViewCnt(조회수 증가) 메소드 추가
 *  2025.10. 17.		홍현택			readPopularCommunityList, readPopularCommunityTotalRecord 추가. 인기글
 *  2025.10. 24.		홍현택			getCategoryCounts 게시글 카운트추가
 * </pre>
 */
@Service
public interface BoardService {

	////////////////////////////////공지사항 ////////////////////////////////
	/** 공지사항 다건 조회 (페이징 O)
	 * @param paging
	 * @return
	 */
	public List<BoardVO> readNoticeList(PaginationInfo<BoardVO> paging);

	/**
	 * 관리자 대시보드 공지사항 리스트
	 * @return
	 */
	public List<BoardVO> readNotices();
	////////////////////카운트 //////////////////////////////////////////////// 10.24현택 추가
	/**
	 * 커뮤니티 카테고리별 게시물 수 조회
	 * @return 카테고리 코드(String)와 게시물 수(Integer)를 담은 Map
	 */
	public Map<String, Integer> getCategoryCounts();

	////////////////////////////////자유게시판 ////////////////////////////////
	/**
	 * 커뮤니티 게시물 개수 조회
	 * @return
	 */
	public int readCommunityTotalRecord(PaginationInfo<BoardVO> paging, String bbsCtgrCd);
	/**
	 * 사내 커뮤니티 목록조회. (페이징 O)
	 * @param paging
	 * @param bbsCtgrCd
	 * @return 조회 결과 없으면 list.size() == 0.
	 */
	public List<BoardVO> readCommunityList(PaginationInfo<BoardVO> paging, String bbsCtgrCd);

	/**
	 * 인기 커뮤니티 게시물 목록 조회 (조회수 기준, 페이징 O)
	 * @param paging
	 * @param bbsCtgrCd
	 * @return 조회 결과 없으면 list.size() == 0.
	 */
	public List<BoardVO> readPopularCommunityList(PaginationInfo<BoardVO> paging, String bbsCtgrCd);

	/**
	 * 인기 커뮤니티 게시물 개수 조회
	 * @return
	 */
	public int readPopularCommunityTotalRecord(PaginationInfo<BoardVO> paging, String bbsCtgrCd);

	/**
	 * 사내 커뮤니티 목록조회. (페이징 X)
	 * bbsCtgrCd 값이 없으면 전체 조회, 있으면 카테고리별 조회
	 * F102	동호회
	 * F103 경조사
	 * F104	사내활동
	 * F105	건의사항
	 * F106 기타
	 * @return 조회 결과 없으면 list.size() == 0.
	 */
	public List<BoardVO> readCommunityListNonPaging(String bbsCtgrCd);

	////////////////////////////////////////////////////////////////////////
	/**
	 * 조회수 증가
	 * @param pstId 게시물 Id
	 * @return 성공하면 true, 실패하면 false
	 */
	public int modifyViewCnt(String pstId);

	/**
	 * 게시글 단건 조회
	 * @param pstId
	 * @return 조회 결과 없으면 EntityNotFoundException 발생
	 */
	public BoardVO readBoard(String pstId);

	/**
	 * 게시글 작성
	 * @param board
	 * @return 성공하면 true, 실패하면 false
	 */
	public boolean createBoard(BoardVO board);

	/**
	 * 게시글 수정
	 * @param board
	 * @return 성공하면 true, 실패하면 false
	 */
	public boolean modifyBoard(BoardVO board);

	/**
	 * 게시글 삭제
	 * @param pstId 게시글 일련번호
	 * @return 성공하면 true, 실패하면 false
	 */
	public boolean removeBoard(String pstId);

}
