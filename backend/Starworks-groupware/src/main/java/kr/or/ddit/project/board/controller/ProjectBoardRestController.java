package kr.or.ddit.project.board.controller;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import kr.or.ddit.comm.file.service.FileDetailService;
import kr.or.ddit.comm.paging.PaginationInfo;
import kr.or.ddit.comm.paging.SimpleSearch;
import kr.or.ddit.comm.paging.renderer.MazerPaginationRenderer;
import kr.or.ddit.comm.paging.renderer.PaginationRenderer;
import kr.or.ddit.comm.validate.InsertGroup;
import kr.or.ddit.comm.validate.UpdateGroup;
import kr.or.ddit.project.board.service.ProjectBoardService;
import kr.or.ddit.project.comment.service.ProjectBoardCommentService;
import kr.or.ddit.security.CustomUserDetails;
import kr.or.ddit.vo.FileDetailVO;
import kr.or.ddit.vo.ProjectBoardVO;
import kr.or.ddit.vo.UsersVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author 장어진
 * @since 2025. 9. 26.
 * @see
 *
 *      <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 26.     	장어진	          최초 생성
 *  2025.10. 02.     	장어진	          기능 제작을 위한 각종 코드 추가
 *
 *      </pre>
 */
@Slf4j
@RestController
@RequestMapping("/rest/project-board")
@RequiredArgsConstructor
public class ProjectBoardRestController {

	private final ProjectBoardService service;
	private final FileDetailService fdService;
	private final ProjectBoardCommentService pbcService;

	@PutMapping("/vct/{bizPstId}")
	public void modifyViewCnt(@PathVariable String bizPstId) {
		boolean ok = service.modifyViewCnt(bizPstId);
		if (!ok) {

		}
	}
	
//	@GetMapping
//	public List<ProjectBoardVO> readProjectBoardListNonPaging(String bizId){
//		return service.readProjectBoardListNonPaging(bizId);
//	};

	@GetMapping("/read")
	public Map<String, Object> readProjectBoardListPaging(
		@RequestParam(required = false) String bizId,
		@RequestParam(required = false, defaultValue = "1") int page,
		@ModelAttribute("search") SimpleSearch search
	) {
		Map<String, Object> result = new HashMap<>();
		PaginationInfo<ProjectBoardVO> paging = new PaginationInfo<>(5, 5);

		paging.setCurrentPage(page);
		paging.setSimpleSearch(search);

		List<ProjectBoardVO> pbList = service.readProjectBoardListPaging(paging, bizId);

		PaginationRenderer renderer = new MazerPaginationRenderer();
		String pagingHTML = renderer.renderPagination(paging);
		log.info("================> paging : {}", pagingHTML);

		int totalRecord = service.readProjectBoardTotalRecord(paging, bizId);

		result.put("totalRecord", totalRecord);
		result.put("pagingHTML", pagingHTML);
		result.put("pbList", pbList);

		return result;
	}
	
	@GetMapping("/read/{bizPstId}")
	public Map<String, Object> readProjectBoard (
		@PathVariable String bizPstId
	){
		ProjectBoardVO pbVO = service.readProjectBoard(bizPstId);
		List<FileDetailVO> fileList = fdService.readFileDetailList(pbVO.getBizPstFileId());
		
		Map<String, Object> result = new HashMap<>();
		result.put("pbVO", pbVO);
		result.put("fileList", fileList);
		
		return result;
	}

	@PostMapping
	public Map<String, Object> createProjectBoard(
		@Validated(InsertGroup.class) @RequestPart("pbVO") ProjectBoardVO pbVO
		, @RequestPart(value = "files", required = false) MultipartFile[] files
		, Authentication authentication
	) {
		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
		UsersVO realUser = userDetails.getRealUser();

		pbVO.setCrtUserId(realUser.getUserId());
		
	    if(files != null) {
	        pbVO.setFileList(Arrays.asList(files));
	    }

		boolean success = service.createProjectBoard(pbVO);
		Map<String, Object> result = new HashMap<>();
		result.put("success", success);
		return result;
	};

	@PutMapping("/modify")
	public Map<String, Object> modifyProjectBoard(
		@Validated(UpdateGroup.class) @RequestPart("pbVO") ProjectBoardVO pbVO
		, @RequestPart(value = "files", required = false) MultipartFile[] files
	) {
	    if(files != null) {
	        pbVO.setFileList(Arrays.asList(files));
	    }
		
		boolean success = service.modifyProjectBoard(pbVO);
		Map<String, Object> result = new HashMap<>();
		result.put("success", success);
		return result;
	}

	@PutMapping("/remove")
	public Map<String, Object> removeProjectBoard(
		@RequestParam String bizPstId  
	) {
		boolean success = service.removeProjectBoard(bizPstId);
		Map<String, Object> result = new HashMap<>();
		result.put("success", success	);
		return result;
	}
}
