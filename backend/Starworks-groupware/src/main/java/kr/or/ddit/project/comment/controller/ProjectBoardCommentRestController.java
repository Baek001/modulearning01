package kr.or.ddit.project.comment.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import kr.or.ddit.comm.validate.InsertGroupNotDefault;
import kr.or.ddit.project.comment.service.ProjectBoardCommentService;
import kr.or.ddit.security.CustomUserDetails;
import kr.or.ddit.vo.ProjectBoardCommentVO;
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
 *  2025.10.  6.     	장어진	          BoardComment 참고해서 메소드 추가
 *
 *      </pre>
 */
@Slf4j
@RestController
@RequestMapping("/rest/project-board-comment/{bizPstId}")
@RequiredArgsConstructor
public class ProjectBoardCommentRestController {

	private final ProjectBoardCommentService service;

	@GetMapping
	public Map<String, Object> readProjectBoardCommentList(@PathVariable String bizPstId) {
		List<ProjectBoardCommentVO> pbcList = service.readProjectBoardCommentList(bizPstId);

		ProjectBoardCommentVO pbcVO = new ProjectBoardCommentVO();
		pbcVO.setBizPstId(bizPstId);
		int totalCount = service.readProjectBoardCommentTotalCount(pbcVO);

		Map<String, Object> result = new HashMap<>();
		result.put("pbcList", pbcList);
		result.put("totalCount", totalCount);

		return result;
	}

	@PostMapping
	public Map<String, Object> createProjectBoardComment(@PathVariable String bizPstId,
			@RequestParam(required = false) String upBizCmntId,
			@Validated(InsertGroupNotDefault.class) @RequestBody ProjectBoardCommentVO pbcVO,
			Authentication authentication) {
		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
		UsersVO realUser = userDetails.getRealUser();

		pbcVO.setBizPstId(bizPstId);
		pbcVO.setUpBizCmntId(upBizCmntId);
		pbcVO.setCrtUserId(realUser.getUserId());

		boolean success = service.createProjectBoardComment(pbcVO);

		Map<String, Object> result = new HashMap<>();
		result.put("success", success);

		if (success) {
			ProjectBoardCommentVO newComment = new ProjectBoardCommentVO();
			newComment.setBizCmntId(pbcVO.getBizCmntId());
			ProjectBoardCommentVO comment = service.readProjectBoardComment(newComment);
			result.put("comment", comment);
		}

		return result;
	}
	
	@PutMapping
	public Map<String, Object> modifyProjectBoardComment(
        @PathVariable String bizPstId,
        @RequestParam String bizCmntId,
        @RequestBody ProjectBoardCommentVO pbcVO,
        Authentication authentication	
	){
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        UsersVO realUser = userDetails.getRealUser();
        
        pbcVO.setBizCmntId(bizCmntId);
        pbcVO.setLastChgUserId(realUser.getUserId());
        
        boolean success = service.modifyProjectBoardComment(pbcVO);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        
        return result;		
	}
	
    @PutMapping("/remove")
    public Map<String, Object> removeComment(
        @PathVariable String bizPstId,
        @RequestParam String bizCmntId
    ) {
        boolean success = service.removeProjectBoardComment(bizCmntId);
        
        // 삭제한 댓글 정보 가져오기
        ProjectBoardCommentVO pbcVO = new ProjectBoardCommentVO();
        pbcVO.setBizCmntId(bizCmntId);
        ProjectBoardCommentVO comment = service.readProjectBoardComment(pbcVO);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("comment", comment);
        
        return result;
    }
}
