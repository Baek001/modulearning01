package kr.or.ddit.project.mngt.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import kr.or.ddit.comm.code.service.CommonCodeService;
import kr.or.ddit.comm.file.service.FileDetailService;
import kr.or.ddit.comm.validate.UpdateGroup;
import kr.or.ddit.project.member.service.projectMemberService;
import kr.or.ddit.project.mngt.service.projectService;
import kr.or.ddit.security.CustomUserDetails;
import kr.or.ddit.vo.CommonCodeVO;
import kr.or.ddit.vo.FileDetailVO;
import kr.or.ddit.vo.ProjectMemberVO;
import kr.or.ddit.vo.ProjectVO;
import lombok.RequiredArgsConstructor;
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
 *  2025. 10. 01.		김주민			  첨부파일 fileDetailService 기능 추가
 *
 * </pre>
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/projects/edit")
public class ProjectModifyController {

	private final projectService service;
	private final CommonCodeService commonCodeService;
	private final projectMemberService memberService;
	private final FileDetailService fileDetailService;

	/**
     * 프로젝트 수정 페이지로 이동합니다.
     * @param model
     * @return "project/project-edit"
     */
    @GetMapping
    public String projectEdit(
    		@RequestParam String bizId,
    		Model model,
    		@AuthenticationPrincipal CustomUserDetails userDetails
    	) {
    	String loginUserId = userDetails.getUsername();

        // 권한 검증
        List<ProjectMemberVO> memberList = memberService.readProjectMemberList(bizId);
        boolean hasPermission = memberList.stream()
            .anyMatch(member ->
                member.getBizUserId().equals(loginUserId)
                && (member.getBizAuthCd().equals("B101") || member.getBizAuthCd().equals("B102"))
            );

        if (!hasPermission) {
            // 권한 없음 - 상세 페이지로 리다이렉트 또는 에러 처리
            return "redirect:/projects/" + bizId;
        }

     // 프로젝트 정보 조회
        ProjectVO project = service.readProject(bizId);
        project.setMembers(memberList);  // 멤버 정보 설정

     // 기존 첨부파일 목록 조회 - null 체크 추가
        List<FileDetailVO> fileList = new ArrayList<>();
        if (project.getBizFileId() != null && !project.getBizFileId().trim().isEmpty()) {
            try {
                fileList = fileDetailService.readFileDetailList(project.getBizFileId());
                if (fileList == null) {
                    fileList = new ArrayList<>();
                }
            } catch (Exception e) {
                System.err.println("파일 목록 조회 실패: " + e.getMessage());
                fileList = new ArrayList<>();
            }
        }

        // 프로젝트 유형 코드 조회
        List<CommonCodeVO> projectTypes = commonCodeService.readCommonCodeList("B2");

        // 프로젝트 상태 코드 조회
        List<CommonCodeVO> projectStatus = commonCodeService.readCommonCodeList("B3");

        // 모델에 속성 추가
        model.addAttribute("project", project);
        model.addAttribute("fileList", fileList);
        model.addAttribute("projectTypes", projectTypes);
        model.addAttribute("projectStatus", projectStatus);
        return "project/project-edit";
    }

    /**
     * 프로젝트 정보를 수정하는 컨트롤러
     * @param project 프로젝트 데이터
     * @param errors 입력값 유효성 검사 결과를 담는 객체
     * @return 처리 결과와 메시지를 포함하는 JSON 응답
     *
     * - 성공 시: HTTP 200 (OK)
     * - 유효성 검사 실패 시: HTTP 400 (Bad Request)
     * - 수정 처리 실패 시: HTTP 500 (Internal Server Error)
     */
    @PostMapping
    @ResponseBody
    public ResponseEntity<Map<String, Object>> formProcess(
        @Validated(UpdateGroup.class) @ModelAttribute ProjectVO project,
        BindingResult errors
    ) {
        Map<String, Object> result = new HashMap<>();

        if(errors.hasErrors()) {
            // 필드별 에러 메시지를 맵으로 변환
            Map<String, String> errorMap = new HashMap<>();
            errors.getFieldErrors().forEach(error -> {
                errorMap.put(error.getField(), error.getDefaultMessage());
            });

            result.put("success", false);
            result.put("errors", errorMap);  // errors 추가
            result.put("message", "입력값을 확인해주세요.");
            return ResponseEntity.badRequest().body(result);
        }

        boolean success = service.modifyProject(project);

        result.put("success", success);
        result.put("bizId", project.getBizId());
        result.put("message", success ? "프로젝트가 수정되었습니다." : "프로젝트 수정에 실패했습니다.");

        return success ? ResponseEntity.ok(result) :
                        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
    }
}

