package kr.or.ddit.mybatis.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import kr.or.ddit.comm.paging.PaginationInfo;
import kr.or.ddit.vo.MenuVO;

/**
 * 
 * @author 김주민
 * @since 2025. 9. 25.
 * @see MenuMapper
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 25.     	김주민	          최초 생성
 *  2025. 9. 26.        김주민            페이징 List 추가 
 * </pre>
 */
@Mapper
public interface MenuMapper {
	int selectTotalRecord(PaginationInfo<MenuVO> paging);
	
	/**
	 * 신제품 메뉴 목록 조회 (페이징 O)
	 * @param paging
	 * @return
	 */
	public List<MenuVO> selectMenuList(PaginationInfo<MenuVO> paging);
	
	
	/**
	 * 신제품 메뉴 등록
	 * @param newMenu
	 * @return
	 */
	public int insertMenu(MenuVO newMenu);
	
	/**
	 * 신제품 메뉴 목록 조회 (페이징 X)
	 * @return 조회 결과 없으면 list.size() == 0
	 */
	public List<MenuVO> selectMenuListNonPaging();
	
	/**
	 * 신제품 메뉴 단건 조회
	 * @param menuId
	 * @return 조회 결과 없으면 null
	 */
	public MenuVO selectMenu(String menuId);
	
	/**
	 * 신제품 메뉴 수정
	 * @param menu
	 * @return 
	 */
	public int updateMenu(MenuVO menu);
	
	/**
	 * 신제품 메뉴 삭제(취소)
	 * @param menuId
	 * @return
	 */
	public int deleteMenu(String menuId);
	
}
