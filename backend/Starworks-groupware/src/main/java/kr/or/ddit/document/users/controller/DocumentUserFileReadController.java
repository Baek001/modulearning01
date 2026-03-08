package kr.or.ddit.document.users.controller;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import kr.or.ddit.comm.file.FileIcon;
import kr.or.ddit.comm.paging.PaginationInfo;
import kr.or.ddit.comm.paging.SimpleSearch;
import kr.or.ddit.comm.paging.renderer.MazerPaginationRenderer;
import kr.or.ddit.comm.paging.renderer.PaginationRenderer;
import kr.or.ddit.document.users.service.DocumentUserFileFolderService;
import kr.or.ddit.document.users.service.DocumentUserFileService;
import kr.or.ddit.security.CustomUserDetails;
import kr.or.ddit.vo.DepartmentFileMappingVO;
import kr.or.ddit.vo.UserFileFolderVO;
import kr.or.ddit.vo.UserFileMappingVO;
import kr.or.ddit.vo.UsersVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author 장어진
 * @since 2025. 10. 9.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 8.     	장어진	          최초 생성
 *
 * </pre>
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class DocumentUserFileReadController {

	public final DocumentUserFileService service;
	public final DocumentUserFileFolderService uffService;

    /**
     * 개인문서 목록 페이지로 이동합니다.
     * @param model
     * @return "document/document_list"
     */
    @GetMapping({"/document/user", "/document/user/{foldersqn}"})
    public String documentUserList(
    	Model model
    	, @RequestParam(name = "page", required = false, defaultValue = "1") int page
    	, @PathVariable(name = "foldersqn", required = false) Integer folderSqn
    	, @ModelAttribute("search") SimpleSearch search
    	, Authentication authentication
    ) {

        PaginationInfo<UserFileMappingVO> paging = new PaginationInfo<>(7, 5); // 페이징
		paging.setCurrentPage(page); // 페이징
		paging.setSimpleSearch(search); // 검색

		// 폴더에 해당하는 파일과 폴더 안에있는 폴더들을 가져옴
		List<UserFileMappingVO> userFileList = service.readUserFileMappingList(folderSqn, paging);
		// 파일 아이콘 세팅
    	for(UserFileMappingVO userFile : userFileList) {
    		String fileIcon = FileIcon.getFileIcon(userFile.getFileDetailVO().getFileMimeType());
    		userFile.setFileIconClass(fileIcon);
    	}

		List<UserFileFolderVO> userFileFolderList = uffService.retrieveFolderList(folderSqn);
		// 최상위폴더부터 현재폴더의 이름과 sqn 을 가져옴
		List<UserFileFolderVO> folderList = new ArrayList<>();
		if (folderSqn != null) {
			folderList = uffService.readPathView(folderSqn);
		}
		int totalRecord = service.retrieveUserFileMappingTotalRecord(paging, folderSqn); // 지금 안 씀

		PaginationRenderer renderer = new MazerPaginationRenderer(); // 페이징
		String pagingHTML = renderer.renderPagination(paging); // 페이징

		// 파일 아이콘 세팅
    	for(UserFileMappingVO userFile : userFileList) {
    		String fileIcon = FileIcon.getFileIcon(userFile.getFileDetailVO().getFileMimeType());
    		userFile.setFileIconClass(fileIcon);
    	}

    	// 현재폴더가 최상위 폴더가 아니라면 현재 폴더 정보 얻어옴 (상위폴더 ... 표시 )
    	if(folderSqn != null) {
    		UserFileFolderVO currentFolder = uffService.readFileFolder(folderSqn); // 현재 폴더 정보
    	    model.addAttribute("currentFolder", currentFolder);
    	}

	    model.addAttribute("currentFolderSqn", folderSqn); // 현재 폴더 sqn
		model.addAttribute("userFileList", userFileList); // 현재 폴더의 파일 리스트
		model.addAttribute("userFolderList", userFileFolderList); // 현재 폴더의 하위 폴더 리스트
		model.addAttribute("folderList", folderList); // 상위폴더부터 현재폴더의 이름과 sqn

		model.addAttribute("totalRecord", totalRecord); // 지금 안 씀
		model.addAttribute("pagingHTML", pagingHTML); // 지금 안 씀

		model.addAttribute("documentType", "user"); // 현재 개인문서함인지, 부서/전사문서함인지 판단

        return "document/document-user";
    }
}
