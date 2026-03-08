package kr.or.ddit.menu.atrz.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kr.or.ddit.comm.file.FileFolderType;
import kr.or.ddit.comm.file.service.impl.FileUploadServiceImpl;
import kr.or.ddit.menu.atrz.service.NewMenuAtrzService;
import kr.or.ddit.menu.service.MenuService;
import kr.or.ddit.vo.MenuVO;
import kr.or.ddit.vo.NewMenuAtrzVO;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author 임가영
 * @since 2025. 9. 26.
 * @see
 *
 *      <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 26.     	임가영	       최초 생성
 *  2025.10. 13.        윤서현		   신제품 메뉴 등록
 *
 *      </pre>
 */
@RestController
@RequestMapping("/rest/product-newmenu")
@RequiredArgsConstructor
public class NewMenuAtrzRestController {

	private final NewMenuAtrzService service;
	private final MenuService menuService;
	private final FileUploadServiceImpl fileUploadService;

	/**
	 * 신메뉴 기안 목록 전체 조회. (페이징 X). RestController
	 *
	 * @return 조회 결과 없으면 list.size() == 0.
	 */
	@GetMapping
	public List<NewMenuAtrzVO> readNewMenuAtrzNonPaging() {
		return service.readNewMenuAtrzNonPaging();
	}

	/**
	 * 신메뉴 기안 목록 단건 조회. RestController
	 *
	 * @param nwmnSqn 신제품 일련번호
	 * @return 조회 결과 없으면 EntityNotFoundException
	 */
	@GetMapping("/{nwmnSqn}")
	public NewMenuAtrzVO readNewMenuAtrz(@PathVariable("nwmnSqn") int nwmnSqn) {
		return service.readNewMenuAtrz(nwmnSqn);
	}


	/**
	 * 신메뉴 기안서완료된 메뉴 등록(추가)
	 * @param vo
	 * @return
	 */
	@PostMapping("/add")
	public String addMenu(@ModelAttribute NewMenuAtrzVO vo) {
		fileUploadService.saveFileS3(vo, FileFolderType.PRODUCT.toString());
	    //System.out.println("파일 ID: " + vo.getMenuFileId());
		System.out.println("[DEBUG] categoryNm=" + vo.getCategoryNm());
	    System.out.println("[DEBUG] priceAmt=" + vo.getPriceAmt());
		MenuVO menu = new MenuVO();
		menu.setMenuNm(vo.getMenuNm());
		menu.setCategoryNm(vo.getCategoryNm());
		menu.setPriceAmt(vo.getPriceAmt());
		menu.setMarketingContent(vo.getMarketingContent());
		menu.setIngredientContent(vo.getIngredientContent());
		menu.setCostRatioAmt(vo.getCostRatioAmt());
		menu.setReleaseYmd(vo.getReleaseYmd());
		menu.setMenuFileId(vo.getMenuFileId());
		//System.out.println("파일 ID: " + vo.getMenuFileId());

		boolean result = menuService.createMenu(menu);
		return result ? "success" : "fail";
	}

}
