package kr.or.ddit.menu.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import kr.or.ddit.comm.paging.PaginationInfo;
import kr.or.ddit.comm.paging.SimpleSearch;
import kr.or.ddit.comm.paging.renderer.MazerPaginationRenderer;
import kr.or.ddit.comm.paging.renderer.PaginationRenderer;
import kr.or.ddit.menu.atrz.service.NewMenuAtrzService;
import kr.or.ddit.menu.service.MenuService;
import kr.or.ddit.vo.MenuVO;
import kr.or.ddit.vo.NewMenuAtrzVO;

/**
 *
 * @author 김주민
 * @since 2025. 9. 26.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 26.     	김주민	          최초 생성
 *  2025. 9. 27.        김주민          products 페이징 처리 추가
 *  2025. 9. 30.		임가영		  상품 이미지 불러오는 로직 추가
 *
 * </pre>
 */
@Controller
@RequestMapping("/products")
public class ProductsController {

	@Autowired
	private MenuService service;

	@Autowired
	private NewMenuAtrzService newMenuAtrzService;

	/**
	 * 신제품 관리 페이지로 이동, 페이징 처리
	 * @param model
	 * @return
	 */
	@GetMapping
	public String products(
			@RequestParam(required = false, defaultValue = "1") int page,
			@ModelAttribute("search") SimpleSearch search,
			Model model
		) {
		PaginationInfo<MenuVO> paging = new PaginationInfo<>(8,5);
		paging.setCurrentPage(page);
		paging.setSimpleSearch(search);

		List<MenuVO> menuList = service.readMenuList(paging);
		PaginationRenderer renderer = new MazerPaginationRenderer();
		String pagingHTML = renderer.renderPagination(paging, "fnPaging");

		model.addAttribute("menuList", menuList);
		model.addAttribute("pagingHTML", pagingHTML);
		model.addAttribute("paging", paging);
        model.addAttribute("search", search);
        return "products/menu-list";
	}

	/**
	 * 제품 기안서 목록으로 이동합니다.
	 * @param model
	 * @return "products/products-proposals"
	 */
	@GetMapping("/proposals")
	public String productsProposals(Model model) {

		List<NewMenuAtrzVO> approvedList = newMenuAtrzService.readNewMenuAtrzNonPaging();

	    model.addAttribute("approvedList", approvedList);
		return "products/products-proposals";
	}
}
