package kr.or.ddit.board.community.service;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import kr.or.ddit.comm.exception.EntityNotFoundException;
import kr.or.ddit.comm.file.FileFolderType;
import kr.or.ddit.comm.file.service.impl.FileDeleteServiceImpl;
import kr.or.ddit.comm.file.service.impl.FileUploadServiceImpl;
import kr.or.ddit.comm.paging.PaginationInfo;
import kr.or.ddit.mybatis.mapper.BoardCommentMapper;
import kr.or.ddit.mybatis.mapper.BoardMapper;
import kr.or.ddit.mybatis.mapper.FileMasterMapper;
import kr.or.ddit.vo.BoardCommentVO;
import kr.or.ddit.vo.BoardVO;
import kr.or.ddit.vo.FileMasterVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
 *  2025. 9. 25.     	홍현택	        공지사항 게시판 서비스 구현체 작성
 *  2025. 9. 25			임가영			공지사항과 자유게시판 조회 분리 / 자유게시판 crud 구현체 생성
 *  2025. 9. 26.     	홍현택	        readNoticeList 수정. 페이징 하게 주석 풀었음.
 *  2025. 9. 27.		임가영			processBoardFile 메소드(파일 업로드) 추가 / updateViewCnt 메소드(조회수 증가) 추가
 *  2025.10. 03.		홍현택			검색 조건을 위한 공지사항 조회 메서드 수정 / selectNoticeTotalRecord 메소드
 *  2025.10. 17.		홍현택			인기글 모아보기 구현체 추가 / readPopularCommunityTotalRecord readPopularCommunityList
 *
 * </pre>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BoardServiceImpl implements BoardService {

	private final BoardMapper mapper;
	private final FileMasterMapper fileMasterMapper;
	private final BoardCommentMapper boardCommentMapper;

	private final FileUploadServiceImpl fileUploadService;
	private final FileDeleteServiceImpl fileDeleteServiceImpl;

	@Value("${file-info.file.path}")
	private Resource fileFolderRes;
	@Value("${file-info.file.path}")
	private File fileFolder;

	////////////////////////////////공지사항 ////////////////////////////////
	/**
	 * 페이징 처리한 공지사항 목록 출력
	 */

	/**
	 * 공지사항 다건 조회 (페이징 O)
	 */
	@Override
	public List<BoardVO> readNoticeList(PaginationInfo<BoardVO> paging) {
		if (paging == null) {
			return mapper.selectNoticeListNonPaging();
		}
		int totalRecord = mapper.selectNoticeTotalRecord(paging);
		paging.setTotalRecord(totalRecord);
		return mapper.selectNoticeList(paging);
	}

	@Override
	public List<BoardVO> readNotices() {
		return mapper.selectNotices();
	}


	////////////////////////////////자유게시판 ////////////////////////////////
	/**
	 * 커뮤니티 게시물 개수 조회
	 * @return
	 */
	public int readCommunityTotalRecord(PaginationInfo<BoardVO> paging, String bbsCtgrCd) {
		Map<String, Object> paramMap = new HashMap<>();
		paramMap.put("paging", paging);
		paramMap.put("bbsCtgrCd", bbsCtgrCd);
		return mapper.selectCommunityTotalRecord(paramMap);
	}

	/**
	 * 사내 커뮤니티 목록조회. (페이징 O)
	 */
	public List<BoardVO> readCommunityList(PaginationInfo<BoardVO> paging, String bbsCtgrCd) {
		Map<String, Object> paramMap = new HashMap<>();
		paramMap.put("paging", paging);
		paramMap.put("bbsCtgrCd", bbsCtgrCd);

		int totalRecord = mapper.selectCommunityTotalRecord(paramMap);
		paging.setTotalRecord(totalRecord);

		paramMap.put("paging", paging);
		return mapper.selectCommunityList(paramMap);
	}

	/**
	 * 인기글 모아보기 리스트 조회 페이징용
	 */
	@Override
	public int readPopularCommunityTotalRecord(PaginationInfo<BoardVO> paging, String bbsCtgrCd) {
		Map<String, Object> paramMap = new HashMap<>();
		paramMap.put("paging", paging);
		if ("popular".equals(bbsCtgrCd)) {
			paramMap.put("bbsCtgrCd", null);
		} else {
			paramMap.put("bbsCtgrCd", bbsCtgrCd);
		}
		return mapper.selectPopularCommunityTotalRecord(paramMap);
	}

	/**
	 * 인기글 모아보기 리스트 조회
	 */
	@Override
	public List<BoardVO> readPopularCommunityList(PaginationInfo<BoardVO> paging, String bbsCtgrCd) {
		// 조회수로 인기글 조회
		Map<String, Object> paramMap = new HashMap<>();
		paramMap.put("paging", paging);
		if ("popular".equals(bbsCtgrCd)) {
			paramMap.put("bbsCtgrCd", null);
		} else {
			paramMap.put("bbsCtgrCd", bbsCtgrCd);
		}

		int totalRecord = mapper.selectPopularCommunityTotalRecord(paramMap);
		paging.setTotalRecord(totalRecord);

		paramMap.put("paging", paging);
		return mapper.selectPopularCommunityList(paramMap);
	}

	/**
	 * 사내 커뮤니티 목록조회. (페이징 X)
	 */
	@Override
	public List<BoardVO> readCommunityListNonPaging(String bbsCtgrCd) {
		return mapper.selectCommunityListNonPaging(bbsCtgrCd);
	}

	/**
	 * 커뮤니티 게시글 갯수 카운트.
	 */
	@Override
	public Map<String, Integer> getCategoryCounts() {
		List<Map<String, Object>> countsList = mapper.selectCategoryCounts();
		Map<String, Integer> categoryCounts = new HashMap<>();
		int total = 0;
		for (Map<String, Object> map : countsList) {
			String category = (String) map.get("category");
			Integer count = ((Number) map.get("count")).intValue();
			categoryCounts.put(category, count);
			total += count;
		}
		categoryCounts.put("total", total);
		return categoryCounts;
	}


	////////////////////////////////////////////////////////////////////////
	/**
	 * 단건으로 공지사항 목록 조회
	 */
	@Override
	public BoardVO readBoard(String pstId) {
		BoardVO board = mapper.selectBoard(pstId);
		if(board == null) {
			throw new EntityNotFoundException(board);
		}

		return board;
	}

	/**
	 * 조회수 증가
	 */
	@Override
	public int modifyViewCnt(String pstId) {
		return mapper.updateViewCnt(pstId);
	}

	/**
	 * 게시글 작성
	 */
	@Override
	public boolean createBoard(BoardVO board) {
		fileUploadService.saveFileS3(board, FileFolderType.BOARD.toString()); // board VO 안에 있는 Multipart 데이터를 board 폴더에 저장한다
		return mapper.insertBoard(board) > 0;
	}

	/**
	 * 게시글 수정
	 */
	@Override
	public boolean modifyBoard(BoardVO board) {
		// 만약 첨부파일이 있으면 기존 게시글 파일 Id 가져와서 삭제여부 Y 로 변경
		if(board.getFileList() != null && !board.getFileList().isEmpty()) {
			BoardVO existBoard = mapper.selectBoard(board.getPstId());
			FileMasterVO fileMaster = new FileMasterVO();
			fileMaster.setFileId(existBoard.getPstFileId());
			fileMasterMapper.updateFileMasterDelyn(fileMaster);
		}
		fileUploadService.saveFileS3(board, "board");
		return mapper.updateBoard(board) > 0;
	}

	/**
	 * 게시글 삭제
	 */
	@Override
	public boolean removeBoard(String pstId) {
		// 기존 게시글 파일 Id 가져와서 만약 있으면 삭제여부 Y로 변경
		BoardVO existBoard = mapper.selectBoard(pstId);
		String fileId = existBoard.getPstFileId();

		fileDeleteServiceImpl.deleteFileDB(fileId);

		// 댓글 가져와서 만약 있으면 삭제여부 Y로 변경
		List<BoardCommentVO> boardCommentList = boardCommentMapper.selectBoardCommentList(pstId);
		for(BoardCommentVO boardComment : boardCommentList) {
			boardCommentMapper.deleteBoardComment(boardComment.getCmntSqn());
		}

		return mapper.deleteBoard(pstId) > 0;

	}




}
