package kr.or.ddit.board.notice.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import kr.or.ddit.board.community.service.BoardService;
import kr.or.ddit.comm.file.service.FileDetailService;
import kr.or.ddit.comm.paging.PaginationInfo;
import kr.or.ddit.comm.paging.SimpleSearch;
import kr.or.ddit.comm.paging.renderer.MazerPaginationRenderer;
import kr.or.ddit.comm.paging.renderer.PaginationRenderer;
import kr.or.ddit.vo.BoardVO;
import kr.or.ddit.vo.FileDetailVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;



/**
 *
 * @author 홍현택
 * @since 2025. 9. 27.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           	수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 27.     	홍현택	        최초 생성
 *	2025. 10. 1.		임가영			사이드바 메뉴 고정하는 코드 추가
 *	2025. 10. 2. 		홍현택			검색기능을 위해 model 데이터 추가
 *	2025.10. 16			홍현택			공지사항 조회수 기능 추가
 * </pre>
 */
@RequiredArgsConstructor
@Slf4j
@Controller
@RequestMapping("/board")
public class BoardNoticeReadController {
	@Autowired
	private BoardService service;
	private final FileDetailService fileDetailService;

	/**
	 * 공지사항 페이지로 이동, 페이징 처리 MazerPaginationRenderer
	 * @param model
	 * @return
	 */
	@GetMapping("/notice")
	public String CommunityNoticeMain(
		@RequestParam(required = false, defaultValue = "1") int page,
		@ModelAttribute("search") SimpleSearch search,
		Model model
	) {
		PaginationInfo<BoardVO> paging = new PaginationInfo<>();
		paging.setCurrentPage(page);
		paging.setSimpleSearch(search);

		// 카테고리별 게시물 수 조회 10.24 현택 추가
		Map<String, Integer> categoryCounts = service.getCategoryCounts();
		model.addAttribute("categoryCounts", categoryCounts);

		List<BoardVO> noticeList = service.readNoticeList(paging);
		PaginationRenderer renderer = new MazerPaginationRenderer();
		String pagingHTML = renderer.renderPagination(paging, "fnBoardPaging");

		model.addAttribute("noticeList", noticeList);
		model.addAttribute("pagingHTML", pagingHTML);
		model.addAttribute("paging", paging);
		return "community/community-notice-list";
	}

	/**
	 * 공지사항 상세 페이지로 이동
	 * @param model
	 * @return
	 */
	@GetMapping("/notice/{postSn}")
	public String CommunityNoticeDetail(Model model, @PathVariable(name = "postSn") String postSn) {
		service.modifyViewCnt(postSn);
		BoardVO board = service.readBoard(postSn);
		List<FileDetailVO> fileList = fileDetailService.readFileDetailList(board.getPstFileId());

		// 카테고리별 게시물 수 조회 10.24 현택 추가
		Map<String, Integer> categoryCounts = service.getCategoryCounts();
		model.addAttribute("categoryCounts", categoryCounts);

		model.addAttribute("board", board);
		model.addAttribute("fileList", fileList);
		return "community/community-notice-detail";
	}
}

