package kr.or.ddit.document.users.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import kr.or.ddit.document.exception.NotAccessFileRemoveException;
import kr.or.ddit.document.users.service.DocumentUserFileFolderService;
import kr.or.ddit.document.users.service.DocumentUserFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author 장어진
 * @since 2025. 10. 10.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 10.     	장어진	          최초 생성
 *  2025. 10. 18.		임가영			  폴더 삭제도 가능하도록 코드 추가
 *
 * </pre>
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class DocumentUserFileRemoveController {

	private final DocumentUserFileService service;
	private final DocumentUserFileFolderService folderService;

	@PostMapping("/document/user/remove")
	public String removeUserFile(
		// 삭제할 파일 리스트
		@RequestParam(required = false, name = "fileId") List<String> fileIds
		, @RequestParam(required = false, name = "fileSeq") List<Integer> fileSeqs
		// 삭제할 폴더 리스트
		, @RequestParam(required = false, name = "folderSqn") List<Integer> folderSqns
		, @RequestParam(required = false, name = "type") String type
		, RedirectAttributes redirectAttributes
	) {
		try {
			// 삭제할 파일이 있다면
			if (fileSeqs != null && !fileSeqs.isEmpty()) {
				try {
					service.removeUserFileMappingByFile(fileIds, fileSeqs);
				} catch (NotAccessFileRemoveException e) {
					redirectAttributes.addFlashAttribute("icon", "info");
					redirectAttributes.addFlashAttribute("toastMessage", e.getMessage());

					return "redirect:/document/" + type;
				}
			}

			// 삭제할 폴더가 있다면
			if (folderSqns != null && folderSqns.size() != 0) {
				folderService.removeFolder(folderSqns);
			}

			redirectAttributes.addFlashAttribute("icon", "trash");
			redirectAttributes.addFlashAttribute("toastMessage", "파일이 삭제되었습니다");
		} catch (Exception e) {
			e.printStackTrace();
			redirectAttributes.addFlashAttribute("icon", "error");
			redirectAttributes.addFlashAttribute("toastMessage", "파일 삭제에 실패했습니다");
		}

		return "redirect:/document/" + type;
	}
}
