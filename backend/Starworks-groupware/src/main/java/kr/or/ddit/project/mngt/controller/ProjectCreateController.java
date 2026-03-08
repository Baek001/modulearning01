package kr.or.ddit.project.mngt.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import kr.or.ddit.comm.code.service.CommonCodeService;
import kr.or.ddit.comm.validate.InsertGroup;
import kr.or.ddit.project.mngt.service.projectService;
import kr.or.ddit.security.CustomUserDetails;
import kr.or.ddit.vo.CommonCodeVO;
import kr.or.ddit.vo.ProjectMemberVO;
import kr.or.ddit.vo.ProjectVO;
import kr.or.ddit.vo.UsersVO;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author 김주민
 * @since 2025. 9. 30.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 30.     	김주민	          최초 생성
 *  2025.10.  2.		임가영			보완 주석 추가
 * </pre>
 */
@Slf4j
@Controller
@RequestMapping("/projects/create")
public class ProjectCreateController {

	@Autowired
	private projectService service;

	@Autowired
	private CommonCodeService commonCodeService;

    /**
     * 프로젝트 생성 페이지로 이동합니다.
     * @param model
     * @return "project/project-create"
     */
    @GetMapping
    public String projectCreateForm(
    		Model model,
    		@AuthenticationPrincipal CustomUserDetails customUser
    	) {

        //프로젝트 유형 코드 조회
        List<CommonCodeVO> projectTypes = commonCodeService.readCommonCodeList("B2");
        model.addAttribute("projectTypes", projectTypes);

        //로그인 사용자 정보
        UsersVO loginUser = customUser.getRealUser();
        model.addAttribute("loginUser", loginUser);

        return "project/project-create";
    }



    /**
     * 프로젝트 등록 처리 (Ajax 방식)
     * 성공 시 projectId를 반환하고, 프론트엔드에서 모달처리
     * @param project
     * @param errors
     * @return
     */
    @PostMapping
    @ResponseBody
    public ResponseEntity<Map<String, Object>> formProcess(
    	@Validated(InsertGroup.class) @ModelAttribute ProjectVO project,
    	@RequestParam(value = "files", required = false) List<MultipartFile> files,
    	BindingResult errors,
    	@RequestParam(required = false) String membersJson,
    	@AuthenticationPrincipal CustomUserDetails customUser
    ) {
    	// 프로젝트 등록요청 처리 결과를 담음
    	Map<String, Object> result = new HashMap<>();

    	//유효성 검사 실패 시
    	if(errors.hasErrors()) {
    		// 필드별 에러 메시지를 맵으로 변환
    	    Map<String, String> errorMap = new HashMap<>();
    	    errors.getFieldErrors().forEach(error -> {
    	        errorMap.put(error.getField(), error.getDefaultMessage());
    	    });

    		result.put("success", false);
    		result.put("errors", errorMap);
    		result.put("message", "입력값을 확인해주세요."); //** message 는 어디에서 사용?
    		return ResponseEntity.badRequest().body(result);
    	}

    	// 담당자(책임자)는 로그인한 사용자로 자동 설정
        UsersVO loginUser = customUser.getRealUser();
        project.setBizPicId(loginUser.getUserId());

        if(files != null && !files.isEmpty()) {
            project.setFileList(files);
            log.info("업로드된 파일 개수: {}", files.size());
        }

        // JSON 문자열로 받은 멤버 정보 파싱
        List<ProjectMemberVO> members = new ArrayList<>();
        if(membersJson != null && !membersJson.isEmpty()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                members = mapper.readValue(membersJson,
                    new TypeReference<List<ProjectMemberVO>>(){});
            } catch (Exception e) {
                log.error("멤버 JSON 파싱 실패", e);
            }
        }

        // 담당자가 멤버 목록에 없으면 추가
        boolean managerExists = members.stream()
            .anyMatch(m -> m.getBizUserId().equals(loginUser.getUserId()));

        if(!managerExists) {
            ProjectMemberVO manager = new ProjectMemberVO();
            manager.setBizUserId(loginUser.getUserId());
            manager.setBizAuthCd("B101"); // 팀장
            members.add(0, manager); // 맨 앞에 추가
        }

        project.setMembers(members);

    	//프로젝트 등록 (상태:승인대기)
		String projectId = service.createProject(project);

		if(projectId != null) { //null이 아니면 성공
			log.info("프로젝트 등록 성공 - projectId: {}", projectId);
			result.put("success", true); //프로젝트 등록이 성공
            result.put("projectId", projectId);
            result.put("message", "프로젝트가 등록되었습니다."); //성공 메시지
            return ResponseEntity.ok(result);
		}else { // null이면 실패
			log.error("프로젝트 등록 실패 - project: {}", project);
            result.put("success", false);
            result.put("message", "프로젝트 등록에 실패했습니다."); //실패 메시지
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
		}

    }
}
