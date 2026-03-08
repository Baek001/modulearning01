package kr.or.ddit.document.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import kr.or.ddit.document.service.DocumentService;
import kr.or.ddit.document.users.service.DocumentUserFileFolderService;
import kr.or.ddit.security.CustomUserDetails;
import kr.or.ddit.vo.UserFileFolderVO;
import kr.or.ddit.vo.UsersVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author 장어진
 * @since 2025. 10. 13.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 13.     	장어진	          upload 기능 추가를 위한 수정
 *
 * </pre>
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class DocumentController {

    private final DocumentUserFileFolderService uffService;
    private final DocumentService docService;
    
    @PostMapping({"/document/restore", "/document/restore/{foldersqn}"})
    public String restoreDocument(
    	@PathVariable(required = false, name = "foldersqn") String folderSqn
    	// 복구할 파일 리스트
		, @RequestParam(required = false, name = "fileId") List<String> fileIds
		, @RequestParam(required = false, name = "fileSeq") List<Integer> fileSeqs
		// 복구할 폴더 리스트
		, @RequestParam(required = false, name = "folderSqn") List<Integer> folderSqns
		, RedirectAttributes redirectAttributes
    ) {
    	boolean fileRestoreSuccess = true;
    	boolean folderRestoreSuccess = true;
    	
    	// 파일 복구 -> folderSqn 을 null 로 바꾸고 복구
    	if(fileIds != null && fileIds.size() != 0) {
    		fileRestoreSuccess = docService.restoreDocumentFile(fileIds, fileSeqs);
    	}
    	
    	// 폴더 복구 -> 현재 폴더의 upFolderSqn 을 null 로 바꾸고 현재폴더와 하위폴더&파일들을 전부 Y 로 바꿈
    	if(folderSqns != null && folderSqns.size() != 0) {
    		folderRestoreSuccess = docService.restoreDocumentFolder(folderSqns);
    	}
    	
    	if(fileRestoreSuccess && folderRestoreSuccess) {
    		redirectAttributes.addFlashAttribute("icon", "success");
    		redirectAttributes.addFlashAttribute("toastMessage", "파일이 복구되었습니다");
    	} else {
    		redirectAttributes.addFlashAttribute("icon", "error");
    		redirectAttributes.addFlashAttribute("toastMessage", "파일 복구 중 에러가 발생했습니다");
    	}
    	
    	String location = (folderSqn != null) ? "redirect:/document/trash/" + folderSqn : "redirect:/document/trash";
    	return location;
    }
    
}
