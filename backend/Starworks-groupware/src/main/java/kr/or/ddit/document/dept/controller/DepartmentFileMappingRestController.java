package kr.or.ddit.document.dept.controller;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import kr.or.ddit.document.dept.service.DepartmentFileMappingService;
import kr.or.ddit.security.CustomUserDetails;
import kr.or.ddit.vo.DepartmentFileMappingVO;
import kr.or.ddit.vo.UsersVO;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class DepartmentFileMappingRestController {

	private final DepartmentFileMappingService service;

	/**
	 * 부서 문서 전체 목록 조회.
	 * @param deptId 부서 일련번호
	 * @return 조회 결과 없으면 list.size() == 0
	 */
	@GetMapping("/rest/document-depart/{deptId}")
	public List<DepartmentFileMappingVO> readDepartmentFileMappingList(@PathVariable String deptId) {
		return service.readDepartmentFileMappingListNonPaging(deptId);
	}

	@PostMapping("/rest/document-depart/department")
	public Map<String, Object> uploadDepartFile(
		@ModelAttribute("fileList") DepartmentFileMappingVO fileList
		, Authentication authentication
	) {
		boolean success = false;

		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
		UsersVO realUser = userDetails.getRealUser();

		fileList.setDeptId(realUser.getDeptId());
		success = service.createDepartmentFileMapping(fileList);
		return Map.of("success", success);
	}

	@PostMapping("/rest/document-depart/company")
	public Map<String, Object> uploadCompanyFile(
		@ModelAttribute("fileList") DepartmentFileMappingVO fileList
		, BindingResult errors
		, RedirectAttributes redirectAttributes
		, Authentication authentication
	) {
		boolean success = false;

		fileList.setDeptId("DP000000");
		if(!errors.hasErrors()) {
			success = service.createDepartmentFileMapping(fileList);
			redirectAttributes.addFlashAttribute("icon", "success");
			redirectAttributes.addFlashAttribute("toastMessage", "파일 등록에 성공했습니다!");
			return Map.of("success", success);
		} else {
			redirectAttributes.addFlashAttribute("userFile", fileList);
			String errorName = BindingResult.MODEL_KEY_PREFIX + "userFile";
			redirectAttributes.addFlashAttribute(errorName, errors);
			redirectAttributes.addFlashAttribute("toastMessage", "파일 등록에 실패했습니다.");
			redirectAttributes.addFlashAttribute("icon", "error");
			return Map.of("success", success);
		}
	}
}
