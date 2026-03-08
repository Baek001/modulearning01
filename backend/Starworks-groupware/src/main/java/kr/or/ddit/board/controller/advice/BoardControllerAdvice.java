package kr.or.ddit.board.controller.advice;

import java.util.List;
import java.util.Map;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import kr.or.ddit.board.community.service.BoardService;
import kr.or.ddit.comm.code.service.CommonCodeService;
import kr.or.ddit.vo.CommonCodeVO;
import lombok.RequiredArgsConstructor;

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
 *  2025.10. 29.		홍현택		 커뮤니티 글쓰기에서 사이드바 카운트를 가져오기 위한 코드 추가
 *
 * </pre>
 */
@ControllerAdvice(basePackages = "kr.or.ddit.board")
@RequiredArgsConstructor
public class BoardControllerAdvice {
	private final CommonCodeService commCodeService;
	private final BoardService service;


	@ModelAttribute("categoryCodeList")
	public List<CommonCodeVO> categoryCodeList() {
		// 카테고리 가져오기
		return commCodeService.readCommonCodeList("F1");
	}

	// 사이드바 메뉴 고정
	@ModelAttribute("currentMenu")
	public String sidebarCurrentMenu(Model model) {

	// 카테고리별 게시물 수 조회 10.29 현택 추가
	Map<String, Integer> categoryCounts = service.getCategoryCounts();
	model.addAttribute("categoryCounts", categoryCounts);

    return "community";
	}
}
