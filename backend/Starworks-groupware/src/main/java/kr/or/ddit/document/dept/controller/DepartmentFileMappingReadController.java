package kr.or.ddit.document.dept.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

import kr.or.ddit.comm.file.FileIcon;
import kr.or.ddit.comm.paging.PaginationInfo;
import kr.or.ddit.comm.paging.SimpleSearch;
import kr.or.ddit.comm.paging.renderer.MazerPaginationRenderer;
import kr.or.ddit.comm.paging.renderer.PaginationRenderer;
import kr.or.ddit.document.dept.service.DepartmentFileMappingService;
import kr.or.ddit.security.CustomUserDetails;
import kr.or.ddit.vo.DepartmentFileMappingVO;
import kr.or.ddit.vo.UsersVO;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author 임가영
 * @since 2025. 10. 9.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 9.     	임가영	       최초 생성
 *
 * </pre>
 */
@Controller
@RequiredArgsConstructor
public class DepartmentFileMappingReadController {

	private final DepartmentFileMappingService service;

	/**
     * 부서문서 목록 페이지로 이동합니다.
     * @param model
     * @return "document/document_list"
     */
    @GetMapping("/document/depart")
    public String documentDepartList(
    	@RequestParam(defaultValue = "1", required = false) int page,
    	@ModelAttribute(name = "search") SimpleSearch search,
    	Authentication authentication,
    	Model model
    ) {
    	CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
    	UsersVO realUser = userDetails.getRealUser();

    	PaginationInfo<DepartmentFileMappingVO> paging = new PaginationInfo<>(10, 5);
    	// 현재 페이지 세팅
    	paging.setCurrentPage(page);
    	// search 값 세팅
    	paging.setSimpleSearch(search);

    	// 리스트 가져오기
    	List<DepartmentFileMappingVO> departmentFileList = service.readDepartmentFileMappingList(paging, realUser.getDeptId());
    	// 파일 아이콘 세팅
    	for(DepartmentFileMappingVO departmentFile : departmentFileList) {
    		String fileIcon = FileIcon.getFileIcon(departmentFile.getFileMimeType());
    		departmentFile.setFileIconClass(fileIcon);
    	}

    	// paginationRenderer 로 Page UI HTML 생성
    	PaginationRenderer renderer = new MazerPaginationRenderer();
    	String pagingHTML = renderer.renderPagination(paging);

    	model.addAttribute("departmentFileList", departmentFileList);
    	model.addAttribute("pagingHTML", pagingHTML);

    	model.addAttribute("documentType", "department"); // 현재 개인문서함인지, 부서/전사문서함인지 판단

    	return "document/document-depart";
    }

    /**
     * 전사문서 목록 페이지로 이동합니다.
     * @param model
     * @return "document/document_list"
     */
    @GetMapping("/document/company")
    public String documentCompanList(
    	@RequestParam(defaultValue = "1", required = false) int page,
    	@ModelAttribute(name = "search") SimpleSearch search,
    	Authentication authentication,
    	Model model
    ) {

    	PaginationInfo<DepartmentFileMappingVO> paging = new PaginationInfo<>(10, 5);
    	// 현재 페이지 세팅
    	paging.setCurrentPage(page);
    	// search 값 세팅
    	paging.setSimpleSearch(search);

    	// 리스트 가져오기
    	List<DepartmentFileMappingVO> departmentFileList = service.readDepartmentFileMappingList(paging, "DP000000");
    	// 파일 아이콘 세팅
    	for(DepartmentFileMappingVO departmentFile : departmentFileList) {
    		String fileIcon = FileIcon.getFileIcon(departmentFile.getFileMimeType());
    		departmentFile.setFileIconClass(fileIcon);
    	}

    	// paginationRenderer 로 Page UI HTML 생성
    	PaginationRenderer renderer = new MazerPaginationRenderer();
    	String pagingHTML = renderer.renderPagination(paging);

    	model.addAttribute("departmentFileList", departmentFileList);

    	model.addAttribute("pagingHTML", pagingHTML);

    	model.addAttribute("documentType", "company"); // 현재 개인문서함인지, 부서/전사문서함인지 판단

    	return "document/document-company";
    }

}
