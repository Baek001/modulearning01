package kr.or.ddit.menu.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.vo.MenuVO;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author 김주민
 * @since 2025. 9. 25.
 * @see MenuServiceImplTest
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 25.     	김주민	          최초 생성
 *
 * </pre>
 */
@Slf4j
@SpringBootTest
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MenuServiceImplTest {
	
	@Autowired
	MenuService service;
	
	MenuVO menu;
	
	@Order(1)
	@Test
	void testCreateMenu() {
		MenuVO newMenu = new MenuVO();
		newMenu.setMenuNm("더미테스트");
		newMenu.setPriceAmt(2000);
		assertTrue(service.createMenu(newMenu));
	}

	@Order(2)
	@Test
	void testReadMenuList() {
		List<MenuVO> result = service.readMenuListNonPaging();
	    assertNotNull(result, "체크리스트 목록이 null이면 안됩니다");
	}
	
	@Order(3)
	@Test
	void testReadMenu() {
		String menuId = "MENU00000001";
		MenuVO menu = service.readMenu(menuId);
		assertNotNull(menu);
		
		log.info("조회된 메뉴: {}", menu);
		log.info("조회할 메뉴 ID: {}", menuId);
		log.info("메뉴 이름: {}", menu.getMenuNm());
		log.info("카테고리: {}", menu.getCategoryNm());
	}
	
	@Order(4)
	@Test
	void testModifyMenu() {
		assertDoesNotThrow(() -> {
	        //유효한 ID
	        String menuId = "MENU00000002";
	        MenuVO saved = service.readMenu(menuId);
	        
	        //조회된 객체가 null이 아닌지 확인
	        assertNotNull(saved, "테스트를 위한 메뉴 데이터가 존재하지 않습니다.");

	        saved.setCategoryNm("수정된 카테고리명");
	        service.modifyMenu(saved);
	        
	        //수정된 내용을 다시 조회하여 검증
	        MenuVO updated = service.readMenu(saved.getMenuId());
	        assertEquals("수정된 카테고리명", updated.getCategoryNm());
	    });
	}
	
	@Order(5)
	@Test
	void testRemoveMenu() {
		assertTrue(service.removeMenu("MENU00000001"));
	}

}
