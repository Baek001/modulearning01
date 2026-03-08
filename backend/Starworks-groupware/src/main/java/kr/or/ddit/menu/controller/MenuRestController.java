package kr.or.ddit.menu.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kr.or.ddit.menu.service.MenuService;
import kr.or.ddit.vo.MenuVO;
import lombok.RequiredArgsConstructor;

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
 *
 * </pre>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/rest/product-menu")
public class MenuRestController {
	
	private final MenuService service;
	
	/**
	 * 신제품 메뉴 목록 조회(페이징X)
	 * @return 조회 결과 없으면 list.size() == 0
	 */
	@GetMapping
	public List<MenuVO> readMenuListNonPaging() {
		return service.readMenuListNonPaging();
	}
	
	/**
	 * 신제품 메뉴 상세 조회
	 * @param menuId
	 * @return
	 */
	@GetMapping("/{menuId}")
	public MenuVO readMenu(@PathVariable String menuId) {
		return service.readMenu(menuId);
	}
	

	/**
	 * 신제품 메뉴 수정
	 * @param menuId
	 * @param menu
	 * @return
	 */
	@PostMapping("/{menuId}")
	public Map<String, Boolean> modifyMenu(
		@PathVariable String menuId
		, @RequestBody MenuVO menu
	) {
		menu.setMenuId(menuId);
		service.modifyMenu(menu);
		return Map.of("success", true);
	}
	
	/**
	 * 신제품 메뉴 삭제
	 * @param menuId
	 * @return
	 */
	@DeleteMapping("/{menuId}")
	public Map<String, Boolean> deleteMenu(@PathVariable String menuId) {
		boolean success = service.removeMenu(menuId);
		return Map.of("success", success);
	}
}
