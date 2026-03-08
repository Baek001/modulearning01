package kr.or.ddit.mybatis.mapper;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
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
 * @see MenuMapperTest
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
class MenuMapperTest {
	@Autowired
	MenuMapper mapper;
	MenuVO menu;
	
	@BeforeEach
	void setUpBefore() {
		menu= new MenuVO();
		menu.setMenuId("MENU00000013");
		menu.setMenuNm("test");
		menu.setCategoryNm("test");
		menu.setMenuFileId("e3421");
		menu.setPriceAmt(2131);
		menu.setMarketingContent("ewrer");
	}
	
	@Order(1)
	@Test
	void testInsertMenu() {
		MenuVO newMenu = new MenuVO();
		newMenu.setMenuId("MENU00000013");
		newMenu.setMenuNm("test");
		newMenu.setCategoryNm("TEST");
		newMenu.setStandardCd("Z888");
		assertDoesNotThrow(()->{
			int rowcnt = mapper.insertMenu(newMenu);
			assertEquals(1, rowcnt);
		});
	}

	@Order(2)
	@Test
	void testSelectMenuList() {
		assertDoesNotThrow(()-> mapper.selectMenuListNonPaging());
	}
	
	@Order(3)
	@Test
	void testSelectMenu() {
		String menuId = "MENU00000001";
		MenuVO menu = mapper.selectMenu(menuId);
		assertNotNull(menu);
		
		log.info("조회된 메뉴 : {}", menu);
		log.info("조회된 회원 아이디 : {}", menu.getMenuId());
		log.info("조회된 회원의 이름 : {}", menu.getMenuNm());
	}
	
	@Order(4)
	@Test
	void testUpdateMenu() {
		MenuVO menu = mapper.selectMenu("MENU00000001");
		menu.setMenuNm("수정");
		assertEquals(1, mapper.updateMenu(menu));
	}
	
	@Order(5)
	@Test
	void testDeleteMenu() {
		String targetId = "MENU00000001";
		
		//논리적 삭제
		int result = mapper.deleteMenu(targetId);
		assertEquals(1, result, "삭제 업데이트 실행되지 않았습니다.");
		
		//삭제 후 상태 확인
		MenuVO afterDelete = mapper.selectMenu(targetId);
		assertNotNull(afterDelete, "논리적 삭제이므로 데이터는 여전히 존재");
		
		assertEquals("Y", afterDelete.getDelYn(), "상태코드가 삭제 상태로 변경되지 않음.");
	}

}
