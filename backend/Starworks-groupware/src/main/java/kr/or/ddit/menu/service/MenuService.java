package kr.or.ddit.menu.service;

import java.util.List;

import kr.or.ddit.comm.paging.PaginationInfo;
import kr.or.ddit.vo.MenuVO;

/**
 * 
 * @author 김주민
 * @since 2025. 9. 25.
 * @see MenuService
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 25.     	김주민	          최초 생성
 *  2025. 9. 27.        김주민            readMenuList 추가
 *
 * </pre>
 */
public interface MenuService {
	
	/**
	 * 신제품 메뉴 목록 조회(페이징 O)
	 * @param paging
	 * @return
	 */
	public List<MenuVO> readMenuList(PaginationInfo<MenuVO> paging);
	
	/**
	 * 신제품 메뉴 추가
	 * @param newMenu
	 * @return 조회 결과가 없으면 0 으로 false
	 */
	public boolean createMenu(MenuVO newMenu);
	
	/**
	 * 신제품 메뉴 목록 조회 (페이징X)
	 * @return 조회 결과 없으면 list.size() == 0
	 */
	public List<MenuVO> readMenuListNonPaging();
	
	/**
	 * 신제품 메뉴 단건 조회
	 * @param menuId 메뉴 ID
	 * @return 조회 결과 없으면 EntityNotFoundException
	 */
	public MenuVO readMenu(String menuId);
	
	/**
	 * 신제품 메뉴 수정
	 * @param menu
	 * @return 조회 결과가 없으면 0 으로 false
	 */
	public boolean modifyMenu(MenuVO menu);
	
	/**
	 * 신제품 메뉴 삭제
	 * @param menuId
	 * @return 조회 결과가 없으면 0 으로 false
	 */
	public boolean removeMenu(String menuId);
}
