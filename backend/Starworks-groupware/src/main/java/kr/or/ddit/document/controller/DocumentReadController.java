package kr.or.ddit.document.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import kr.or.ddit.comm.file.FileIcon;
import kr.or.ddit.document.dept.service.DepartmentFileMappingService;
import kr.or.ddit.document.users.service.DocumentUserFileFolderService;
import kr.or.ddit.document.users.service.DocumentUserFileService;
import kr.or.ddit.security.CustomUserDetails;
import kr.or.ddit.vo.DepartmentFileMappingVO;
import kr.or.ddit.vo.UserFileFolderVO;
import kr.or.ddit.vo.UserFileMappingVO;
import kr.or.ddit.vo.UsersVO;
import lombok.RequiredArgsConstructor;

/**
 * 휴지통 등 이동.
 * @author 임가영
 * @since 2025. 10. 16.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 16.     	임가영           최초 생성
 *
 * </pre>
 */
@Controller
@RequiredArgsConstructor
public class DocumentReadController {

	private final DocumentUserFileService docUserFileService;
	private final DocumentUserFileFolderService docUserFolderService;
	private final DepartmentFileMappingService deptDocService;

	@GetMapping({"/document/trash", "/document/trash/{foldersqn}"})
	public String readDocumentTrash(
		@PathVariable(name = "foldersqn", required = false) Integer folderSqn
		, Model model
	) {

		// 최상위의 삭제된 하위폴더와 파일 가져오기
		List<UserFileFolderVO> userFolderList = docUserFolderService.readUserFileFolderNonPagingByDelY(folderSqn);
		List<UserFileMappingVO> userFileList = docUserFileService.readUserFileMappingNonPagingByDelY(folderSqn);

		// 개인자료실 파일 아이콘 세팅
    	for(UserFileMappingVO userFile : userFileList) {
    		String fileIcon = FileIcon.getFileIcon(userFile.getFileDetailVO().getFileMimeType());
    		userFile.setFileIconClass(fileIcon);
    	}

		if(folderSqn == null) {
			// 삭제된 부서 & 전사 문서 보여주기
			List<DepartmentFileMappingVO> deptFileList = deptDocService.readDepartmentFileMappingVONonPagingByDelY();
			// 부서 & 전사자료실 파일 아이콘 세팅
	    	for(DepartmentFileMappingVO deptFile : deptFileList) {
	    		String fileIcon = FileIcon.getFileIcon(deptFile.getFileMimeType());
	    		deptFile.setFileIconClass(fileIcon);
	    	}
			model.addAttribute("deptFileList", deptFileList);
		}

		model.addAttribute("currentFolderSqn", folderSqn); // 현재 폴더 sqn
		model.addAttribute("userFolderList", userFolderList);
		model.addAttribute("userFileList", userFileList);

		return "document/document-trash";
	}

	 @GetMapping("/upload")
	    public String uploadForm(
			Model model
			, Authentication authentication
	    ) {
	        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
	        UsersVO realUser = userDetails.getRealUser();
	        String userId = realUser.getUserId();

	        List<UserFileFolderVO> rootFolders = docUserFolderService.retrieveFolderList(null);
	        UserFileFolderVO rootFolder = rootFolders.stream()
	            .filter(f -> "DEFAULT".equals(f.getFolderType()))
	            .findFirst()
	            .orElse(null);

	        List<UserFileFolderVO> allFolders = docUserFolderService.retrieveAllFolderList(userId);

	        model.addAttribute("rootFolder", rootFolder);
	        model.addAttribute("allFolders", allFolders);

	        return "document/document-upload";
	    }
}
