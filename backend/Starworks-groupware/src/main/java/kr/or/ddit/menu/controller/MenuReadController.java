package kr.or.ddit.menu.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import kr.or.ddit.menu.service.MenuService;
import kr.or.ddit.vo.MenuVO;

//@Controller
//@RequestMapping("/products")
public class MenuReadController {
	@Autowired
	private MenuService service;
	
	@GetMapping
	public String menuListNonPaging(Model model) {
		List<MenuVO> menuList = service.readMenuListNonPaging();
		model.addAttribute("menu",menuList);
		return "products/menu";
	}
}
