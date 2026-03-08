package kr.or.ddit.project.mngt.controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import kr.or.ddit.comm.paging.PaginationInfo;
import kr.or.ddit.comm.paging.renderer.MazerPaginationRenderer;
import kr.or.ddit.comm.paging.renderer.PaginationRenderer;
import kr.or.ddit.mybatis.mapper.ProjectMapper;
import kr.or.ddit.mybatis.mapper.ProjectMemberMapper;
import kr.or.ddit.project.mngt.service.projectService;
import kr.or.ddit.security.CustomUserDetails;
import kr.or.ddit.task.main.service.MainTaskService;
import kr.or.ddit.vo.MainTaskVO;
import kr.or.ddit.vo.ProjectMemberVO;
import kr.or.ddit.vo.ProjectVO;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/rest/project")
public class ProjectRestController {

	private static final Set<String> ALLOWED_PROJECT_STATUSES = Set.of("B301", "B302", "B303", "B304", "B305");
	private static final Set<String> ALLOWED_TASK_STATUSES = Set.of("B401", "B402", "B403", "B404");
	private static final Set<String> PROJECT_EDITABLE_ROLES = Set.of("B101", "B102");
	private static final Set<String> PROJECT_VISIBLE_ROLES = Set.of("B101", "B102", "B103");

	private final projectService service;
	private final ProjectMapper projectMapper;
	private final ProjectMemberMapper projectMemberMapper;
	private final MainTaskService mainTaskService;

	@GetMapping("/{bizId}/members")
	public List<ProjectMemberVO> getProjectMembers(
		@PathVariable String bizId,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		requireProjectAccess(bizId, userDetails);
		return projectMemberMapper.selectProjectMemberByProject(bizId);
	}

	@GetMapping
	public List<ProjectVO> readProjectList(
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		return projectMapper.selectProjectListForReact(requireUserId(userDetails));
	}

	@GetMapping("/{bizId}")
	public Map<String, Object> readProject(
		@PathVariable String bizId,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		ProjectVO project = requireProjectAccess(bizId, userDetails);
		String userId = requireUserId(userDetails);
		String currentUserAuthCd = resolveProjectAuthority(project, userId);

		return Map.of(
			"project", project,
			"members", projectMemberMapper.selectProjectMemberByProject(bizId),
			"currentUserAuthCd", currentUserAuthCd
		);
	}

	@PostMapping
	public Map<String, Object> createProject(
		@RequestBody ProjectVO project,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		String userId = requireUserId(userDetails);

		project.setBizPicId(userId);
		if (project.getBizSttsCd() == null || project.getBizSttsCd().isBlank()) {
			project.setBizSttsCd("B301");
		}
		if (project.getBizPrgrs() == null) {
			project.setBizPrgrs(0);
		}
		project.setMembers(normalizeMembers(project.getMembers(), userId, null));

		String bizId = service.createProject(project);
		return Map.of(
			"success", bizId != null,
			"bizId", bizId
		);
	}

	@PutMapping("/{bizId}")
	public Map<String, Object> modifyProject(
		@PathVariable String bizId,
		@RequestBody ProjectVO project,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		ProjectVO existing = requireProjectAccess(bizId, userDetails);
		String authCd = resolveProjectAuthority(existing, requireUserId(userDetails));
		if (!PROJECT_EDITABLE_ROLES.contains(authCd)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "프로젝트 수정 권한이 없습니다.");
		}

		project.setBizId(bizId);
		project.setBizPicId(existing.getBizPicId());
		if (project.getBizPrgrs() == null) {
			project.setBizPrgrs(existing.getBizPrgrs());
		}
		project.setMembers(normalizeMembers(project.getMembers(), existing.getBizPicId(), bizId));

		boolean success = service.modifyProject(project);
		return Map.of("success", success);
	}

	@PatchMapping("/{bizId}/status")
	public Map<String, Object> updateProjectStatus(
		@PathVariable String bizId,
		@RequestBody Map<String, String> body,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		ProjectVO project = requireProjectAccess(bizId, userDetails);
		String authCd = resolveProjectAuthority(project, requireUserId(userDetails));
		ensureManager(authCd, "프로젝트 상태 변경 권한이 없습니다.");

		String nextStatus = body.get("bizSttsCd");
		if (!ALLOWED_PROJECT_STATUSES.contains(nextStatus)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 프로젝트 상태입니다.");
		}

		boolean success = projectMapper.updateProjectStatusCode(bizId, nextStatus) > 0;
		return Map.of("success", success);
	}

	@PostMapping("/{bizId}/cancel")
	public Map<String, Object> cancelProject(
		@PathVariable String bizId,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		ProjectVO project = requireProjectAccess(bizId, userDetails);
		ensureManager(resolveProjectAuthority(project, requireUserId(userDetails)), "프로젝트 취소 권한이 없습니다.");
		boolean success = projectMapper.updateProjectStatusCode(bizId, "B305") > 0;
		return Map.of("success", success);
	}

	@PatchMapping("/{bizId}/restore")
	public Map<String, Object> restoreProject(
		@PathVariable String bizId,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		ProjectVO project = requireProjectAccess(bizId, userDetails);
		ensureManager(resolveProjectAuthority(project, requireUserId(userDetails)), "프로젝트 복원 권한이 없습니다.");
		boolean success = projectMapper.updateProjectStatusCode(bizId, "B302") > 0;
		return Map.of("success", success);
	}

	@PostMapping("/{bizId}/complete")
	public Map<String, Object> completeProject(
		@PathVariable String bizId,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		ProjectVO project = requireProjectAccess(bizId, userDetails);
		ensureManager(resolveProjectAuthority(project, requireUserId(userDetails)), "프로젝트 완료 처리 권한이 없습니다.");
		boolean success = projectMapper.updateProjectStatusCode(bizId, "B304") > 0;
		return Map.of("success", success);
	}

	@GetMapping("/{bizId}/tasks")
	public Map<String, Object> readProjectTasks(
		@PathVariable String bizId,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		ProjectVO project = requireProjectAccess(bizId, userDetails);
		String currentUserAuthCd = resolveProjectAuthority(project, requireUserId(userDetails));

		return Map.of(
			"tasks", mainTaskService.readMainTaskListNonPaging(bizId),
			"currentUserAuthCd", currentUserAuthCd
		);
	}

	@PostMapping("/{bizId}/tasks")
	public Map<String, Object> createTask(
		@PathVariable String bizId,
		@RequestBody MainTaskVO task,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		ProjectVO project = requireProjectAccess(bizId, userDetails);
		ensureManager(resolveProjectAuthority(project, requireUserId(userDetails)), "업무 생성 권한이 없습니다.");

		task.setBizId(bizId);
		task.setTaskFileId(null);
		if (task.getTaskPrgrs() == null) {
			task.setTaskPrgrs(0);
		}
		if (task.getTaskSttsCd() == null || task.getTaskSttsCd().isBlank()) {
			task.setTaskSttsCd("B401");
		}
		if (!ALLOWED_TASK_STATUSES.contains(task.getTaskSttsCd())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 업무 상태입니다.");
		}

		boolean success = mainTaskService.createMainTask(task);
		return Map.of(
			"success", success,
			"taskId", task.getTaskId()
		);
	}

	@PutMapping("/tasks/{taskId}")
	public Map<String, Object> modifyTask(
		@PathVariable String taskId,
		@RequestBody MainTaskVO task,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		MainTaskVO existing = mainTaskService.readMainTask(taskId);
		ProjectVO project = requireProjectAccess(existing.getBizId(), userDetails);
		ensureManager(resolveProjectAuthority(project, requireUserId(userDetails)), "업무 수정 권한이 없습니다.");

		task.setTaskId(taskId);
		task.setBizId(existing.getBizId());
		if (task.getTaskPrgrs() == null) {
			task.setTaskPrgrs(existing.getTaskPrgrs());
		}
		if (task.getTaskFileId() == null) {
			task.setTaskFileId(existing.getTaskFileId());
		}
		if (task.getTaskSttsCd() == null || task.getTaskSttsCd().isBlank()) {
			task.setTaskSttsCd(existing.getTaskSttsCd());
		}

		boolean success = mainTaskService.modifyMainTask(task);
		return Map.of("success", success);
	}

	@PatchMapping("/tasks/{taskId}/status")
	public Map<String, Object> updateTaskStatus(
		@PathVariable String taskId,
		@RequestBody Map<String, String> body,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		MainTaskVO task = mainTaskService.readMainTask(taskId);
		ProjectVO project = requireProjectAccess(task.getBizId(), userDetails);
		String userId = requireUserId(userDetails);
		String authCd = resolveProjectAuthority(project, userId);
		String nextStatus = body.get("taskSttsCd");

		if (!ALLOWED_TASK_STATUSES.contains(nextStatus)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 업무 상태입니다.");
		}

		if ("B101".equals(authCd)) {
			return Map.of("success", mainTaskService.updateTaskStatus(taskId, nextStatus));
		}

		if ("B102".equals(authCd) && userId.equals(task.getBizUserId())) {
			return Map.of("success", mainTaskService.updateTaskStatus(taskId, nextStatus));
		}

		throw new ResponseStatusException(HttpStatus.FORBIDDEN, "업무 상태 변경 권한이 없습니다.");
	}

	@DeleteMapping("/tasks/{taskId}")
	public Map<String, Object> removeTask(
		@PathVariable String taskId,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		MainTaskVO task = mainTaskService.readMainTask(taskId);
		ProjectVO project = requireProjectAccess(task.getBizId(), userDetails);
		ensureManager(resolveProjectAuthority(project, requireUserId(userDetails)), "업무 삭제 권한이 없습니다.");
		return Map.of("success", mainTaskService.removeMainTask(taskId));
	}

	@GetMapping("/my")
	public Map<String, Object> readMyProjectList(
		@RequestParam(required = false, defaultValue = "1") int page,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		if (userDetails == null) {
			return Map.of("error", "unauthorized");
		}

		String userId = userDetails.getUsername();
		PaginationInfo<ProjectVO> paging = new PaginationInfo<>(6, 5);
		paging.setCurrentPage(page);

		List<ProjectVO> projectList = service.readMyProjectList(userId, paging);
		for (ProjectVO project : projectList) {
			project.setMembers(projectMemberMapper.selectProjectMemberByProject(project.getBizId()));
		}

		PaginationRenderer renderer = new MazerPaginationRenderer();
		String pagingHTML = renderer.renderPagination(paging, "fnProjectPaging");

		return Map.of(
			"projectList", projectList,
			"pagingHTML", pagingHTML,
			"totalCount", paging.getTotalRecord()
		);
	}

	@GetMapping("/my/progress")
	public Map<String, Object> readMyInProgressProjectListRest(
		@RequestParam(required = false, defaultValue = "1") int page,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		if (userDetails == null) {
			return Map.of("error", "unauthorized");
		}

		String userId = userDetails.getUsername();
		PaginationInfo<ProjectVO> paging = new PaginationInfo<>(6, 5);
		paging.setCurrentPage(page);

		List<ProjectVO> projectList = service.readMyProjectList(userId, paging);
		for (ProjectVO project : projectList) {
			project.setMembers(projectMemberMapper.selectProjectMemberByProject(project.getBizId()));
		}

		PaginationRenderer renderer = new MazerPaginationRenderer();
		String pagingHTML = renderer.renderPagination(paging, "fnPaging");

		return Map.of(
			"projectList", projectList,
			"pagingHTML", pagingHTML,
			"totalCount", paging.getTotalRecord()
		);
	}

	@GetMapping("/my/completed")
	public Map<String, Object> readMyCompletedProjectListRest(
		@RequestParam(required = false, defaultValue = "1") int page,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		if (userDetails == null) {
			return Map.of("error", "unauthorized");
		}

		String userId = userDetails.getUsername();
		PaginationInfo<ProjectVO> paging = new PaginationInfo<>(6, 5);
		paging.setCurrentPage(page);

		List<ProjectVO> projectList = service.readMyCompletedProjectList(userId, paging);
		for (ProjectVO project : projectList) {
			project.setMembers(projectMemberMapper.selectProjectMemberByProject(project.getBizId()));
		}

		PaginationRenderer renderer = new MazerPaginationRenderer();
		String pagingHTML = renderer.renderPagination(paging, "fnPaging");

		return Map.of(
			"projectList", projectList,
			"pagingHTML", pagingHTML,
			"totalCount", paging.getTotalRecord()
		);
	}

	private ProjectVO requireProjectAccess(String bizId, CustomUserDetails userDetails) {
		String userId = requireUserId(userDetails);
		ProjectVO project = projectMapper.selectProjectForReact(bizId);
		if (project == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "프로젝트를 찾을 수 없습니다.");
		}

		String authCd = resolveProjectAuthority(project, userId);
		if (!PROJECT_VISIBLE_ROLES.contains(authCd)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "프로젝트 접근 권한이 없습니다.");
		}
		return project;
	}

	private String resolveProjectAuthority(ProjectVO project, String userId) {
		if (project.getBizPicId() != null && project.getBizPicId().equals(userId)) {
			return "B101";
		}

		String authCd = projectMemberMapper.getProjectAuthority(project.getBizId(), userId);
		if (authCd == null || authCd.isBlank()) {
			return "";
		}
		return authCd;
	}

	private void ensureManager(String authCd, String message) {
		if (!"B101".equals(authCd)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, message);
		}
	}

	private String requireUserId(CustomUserDetails userDetails) {
		if (userDetails == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
		}
		return userDetails.getUsername();
	}

	private List<ProjectMemberVO> normalizeMembers(List<ProjectMemberVO> members, String managerUserId, String bizId) {
		LinkedHashMap<String, ProjectMemberVO> normalized = new LinkedHashMap<>();
		normalized.put(managerUserId, createMember(managerUserId, "B101", bizId));

		List<ProjectMemberVO> sourceMembers = members;
		if ((sourceMembers == null || sourceMembers.isEmpty()) && bizId != null) {
			sourceMembers = projectMemberMapper.selectProjectMemberByProject(bizId);
		}

		if (sourceMembers != null) {
			for (ProjectMemberVO member : sourceMembers) {
				if (member == null || member.getBizUserId() == null || member.getBizUserId().isBlank()) {
					continue;
				}
				if (managerUserId.equals(member.getBizUserId())) {
					continue;
				}

				String authCd = "B103".equals(member.getBizAuthCd()) ? "B103" : "B102";
				normalized.put(member.getBizUserId(), createMember(member.getBizUserId(), authCd, bizId));
			}
		}

		return new ArrayList<>(normalized.values());
	}

	private ProjectMemberVO createMember(String userId, String authCd, String bizId) {
		ProjectMemberVO member = new ProjectMemberVO();
		member.setBizId(bizId);
		member.setBizUserId(userId);
		member.setBizAuthCd(authCd);
		return member;
	}
}
