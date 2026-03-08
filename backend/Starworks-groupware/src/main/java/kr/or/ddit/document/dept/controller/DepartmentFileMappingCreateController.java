package kr.or.ddit.document.dept.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import kr.or.ddit.document.dept.service.DepartmentFileMappingService;
import kr.or.ddit.security.CustomUserDetails;
import kr.or.ddit.vo.DepartmentFileMappingVO;
import kr.or.ddit.vo.UsersVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author 임가영
 * @since 2025. 10. 10.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 10.     	임가영           최초 생성
 *
 * </pre>
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class DepartmentFileMappingCreateController {
	public final DepartmentFileMappingService service;

	@PostMapping("/document/upload/department")
	public String uploadDepartFile(
		@ModelAttribute("fileList") DepartmentFileMappingVO fileList
		, BindingResult errors
		, RedirectAttributes redirectAttributes
		, Authentication authentication 
	) {
		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
		UsersVO realUser = userDetails.getRealUser();
		
		fileList.setDeptId(realUser.getDeptId());
		if(!errors.hasErrors()) {
			service.createDepartmentFileMapping(fileList);
			redirectAttributes.addFlashAttribute("icon", "success");
			redirectAttributes.addFlashAttribute("toastMessage", "파일 등록에 성공했습니다!");
			return "redirect:/document/depart";
		} else {
			redirectAttributes.addFlashAttribute("userFile", fileList);
			String errorName = BindingResult.MODEL_KEY_PREFIX + "userFile";
			redirectAttributes.addFlashAttribute(errorName, errors);
			redirectAttributes.addFlashAttribute("toastMessage", "파일 등록에 실패했습니다.");
			redirectAttributes.addFlashAttribute("icon", "error");
			return "redirect:/document/depart";
		}
	}
	
	@PostMapping("/document/upload/company")
	public String uploadCompanyFile(
		@ModelAttribute("fileList") DepartmentFileMappingVO fileList
		, BindingResult errors
		, RedirectAttributes redirectAttributes
		, Authentication authentication 
	) {
		
		fileList.setDeptId("DP000000");
		if(!errors.hasErrors()) {
			service.createDepartmentFileMapping(fileList);
			redirectAttributes.addFlashAttribute("icon", "success");
			redirectAttributes.addFlashAttribute("toastMessage", "파일 등록에 성공했습니다!");
			return "redirect:/document/company";
		} else {
			redirectAttributes.addFlashAttribute("userFile", fileList);
			String errorName = BindingResult.MODEL_KEY_PREFIX + "userFile";
			redirectAttributes.addFlashAttribute(errorName, errors);
			redirectAttributes.addFlashAttribute("toastMessage", "파일 등록에 실패했습니다.");
			redirectAttributes.addFlashAttribute("icon", "error");
			return "redirect:/document/company";
		}
	}
}
