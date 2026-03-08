package kr.or.ddit.board.community.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import kr.or.ddit.board.comment.service.BoardCommentService;
import kr.or.ddit.board.community.service.BoardService;
import kr.or.ddit.comm.file.service.FileDetailService;
import kr.or.ddit.comm.paging.PaginationInfo;
import kr.or.ddit.comm.paging.SimpleSearch;
import kr.or.ddit.comm.paging.renderer.MazerPaginationRenderer;
import kr.or.ddit.comm.paging.renderer.PaginationRenderer;
import kr.or.ddit.vo.BoardCommentVO;
import kr.or.ddit.vo.BoardVO;
import kr.or.ddit.vo.FileDetailVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author 임가영
 * @since 2025. 9. 26.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 26.     	임가영	       최초 생성
 *  2025. 9. 27			홍현택		   공지사항 분리
 *  2025.10. 17.		홍현택			카테고리이름과 코드 매핑
 *  2025.10. 24.		홍현택			getCategoryCounts(게시글 카운트)모델에 추가
 *
 * </pre>
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class BoardCommunityReadController {

	private final BoardService service;
	private final FileDetailService fileDetailService;
	private final BoardCommentService boardCommentservice;

	/**
	 * 사내 자유게시판 진입
	 * @param model
	 * @param category
	 * @return
	 */
	@GetMapping("/board/community")
	public String communityBoardMain(
		Model model
		, @RequestParam(required = false) String category
		, @RequestParam(required = false, defaultValue = "1") int page
		, @ModelAttribute("search") SimpleSearch search
	) {
		log.info("======> category : {}", category);
		PaginationInfo<BoardVO> paging = new PaginationInfo<>(5, 5);

		// 현재 페이지 세팅
		paging.setCurrentPage(page);
		// search 값 세팅
		paging.setSimpleSearch(search);
		List<BoardVO> boardList;
		int totalRecord;

		if ("popular".equals(category)) {
			// 인기글 모아보기
			boardList = service.readPopularCommunityList(paging, category);
			totalRecord = service.readPopularCommunityTotalRecord(paging, category);
		} else {
			// category 에 따른 list 가져오기
			boardList = service.readCommunityList(paging, category);
			totalRecord = service.readCommunityTotalRecord(paging, category);
		}

		// 카테고리 코드-이름 매핑
		Map<String, String> categoryMap = new HashMap<>();
		categoryMap.put("F102", "동호회");
		categoryMap.put("F103", "경조사");
		categoryMap.put("F104", "사내활동");
		categoryMap.put("F105", "건의사항");
		categoryMap.put("F106", "기타");
		model.addAttribute("categoryMap", categoryMap);

		// 카테고리별 게시물 수 조회 10.24 현택 추가
		Map<String, Integer> categoryCounts = service.getCategoryCounts();
		model.addAttribute("categoryCounts", categoryCounts);

		// paginationRenderer 로 Page UI HTML 생성
		PaginationRenderer renderer = new MazerPaginationRenderer();
		String pagingHTML = renderer.renderPagination(paging);

		model.addAttribute("totalRecord", totalRecord);
		model.addAttribute("boardList", boardList);
		model.addAttribute("pagingHTML", pagingHTML);
		return "community/community-board";
	}

	/**
	 * 자유게시판 상세조회
	 * @return
	 */
	@GetMapping("/board/community/{pstId}")
	public String readCommunityDetail(Model model, @PathVariable(name = "pstId") String pstId) {
		BoardVO board = service.readBoard(pstId);
		List<FileDetailVO> fileList = fileDetailService.readFileDetailList(board.getPstFileId());

		BoardCommentVO boardComment = new BoardCommentVO();
		boardComment.setPstId(pstId);
		// 댓글 가져오기
		List<BoardCommentVO> boardCommentList = boardCommentservice.readBoardCommentList(pstId);
		// 댓글 수 가져오기
		int totalCount = boardCommentservice.readBoardCommentTotalCount(boardComment);

		// 카테고리 코드-이름 매핑
		Map<String, String> categoryMap = new HashMap<>();
		categoryMap.put("F102", "동호회");
		categoryMap.put("F103", "경조사");
		categoryMap.put("F104", "사내활동");
		categoryMap.put("F105", "건의사항");
		categoryMap.put("F106", "기타");
		model.addAttribute("categoryMap", categoryMap);

		// 카테고리별 게시물 수 조회 10.24 현택 추가
		Map<String, Integer> categoryCounts = service.getCategoryCounts();
		model.addAttribute("categoryCounts", categoryCounts);

		model.addAttribute("board", board);
		model.addAttribute("fileList", fileList);
		model.addAttribute("boardCommentList", boardCommentList);
		model.addAttribute("totalCount", totalCount);
		return "community/community-board-detail";
	}

}

