package kr.or.ddit.project.mngt.service;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.comm.exception.EntityNotFoundException;
import kr.or.ddit.comm.file.FileFolderType;
import kr.or.ddit.comm.file.service.FileDetailService;
import kr.or.ddit.comm.file.service.FileMasterService;
import kr.or.ddit.comm.file.service.impl.FileUploadServiceImpl;
import kr.or.ddit.comm.paging.PaginationInfo;
import kr.or.ddit.mybatis.mapper.ProjectMapper;
import kr.or.ddit.mybatis.mapper.ProjectMemberMapper;
import kr.or.ddit.users.service.UsersService;
import kr.or.ddit.vo.FileDetailVO;
import kr.or.ddit.vo.FileMasterVO;
import kr.or.ddit.vo.ProjectMemberVO;
import kr.or.ddit.vo.ProjectVO;
import kr.or.ddit.vo.UsersVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author 김주민
 * @since 2025. 9. 25.
 * @see projectServiceImpl
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 25.     	김주민	          최초 생성
 *  2025. 9. 29.     	김주민 			readMyProjectListNonPaging 추가
 *  2025. 10. 01.		김주민			fileService	추가
 *  2025. 10. 11.		김주민			프로젝트 완료 처리 completeProject 추가
 *  2025. 10. 16. 		김주민			내 프로젝트 서비스 분리 -> readMyCompletedProjectList 추가
 *
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class projectServiceImpl implements projectService {

	private final ProjectMapper mapper;
	private final ProjectMemberMapper projectMemberMapper;

	private final FileUploadServiceImpl fileService;
	private final FileMasterService fileMasterService;
	private final FileDetailService fileDetailService;
	private final UsersService userService;

	/**
	 * 프로젝트 완료 처리
	 */
	@Override
	public boolean completeProject(String bizId) {
		int result = mapper.completeProject(bizId);
		return result > 0;
	}

	/**
	 * 보관함 : 취소된 프로젝트 목록 조회 (페이징O)
	 */
	@Override
	public List<ProjectVO> readArchivedProjectList(PaginationInfo<ProjectVO> paging) {
		int totalRecord = mapper.selectArchivedTotalRecord(paging);
		paging.setTotalRecord(totalRecord);

		return mapper.selectArchivedProjectList(paging);
	}

	/**
	 * 프로젝트 데이터를 결재 템플릿용으로 변환
	 */
	@Override
	public Map<String, String> convertProjectToTemplateData(String bizId) {
		//프로젝트 정보 조회 및 맵 초기화
		ProjectVO project = readProject(bizId);
		Map<String, String> data = new HashMap<>();

		try {
			// 책임자 정보 조회
			UsersVO picUser = userService.readUser(project.getBizPicId());
			data.put("${PROJECT_PIC_NM}", picUser.getUserNm() != null ? picUser.getUserNm() : "");
			data.put("${PROJECT_DEPT_NM}", picUser.getDeptNm() != null ? picUser.getDeptNm() : "");
		} catch (Exception e) {
			data.put("${PROJECT_PIC_NM}", project.getBizPicId());
			data.put("${PROJECT_DEPT_NM}", "");
		}

		// 날짜 포맷
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

		//html 플레이스홀더 매핑 & null 체크
		data.put("${PROJECT_NM}", project.getBizNm() != null ? project.getBizNm() : "");
		data.put("${PROJECT_ID}", project.getBizId() != null ? project.getBizId() : "");
		data.put("${START_DATE}", project.getStrtBizDt() != null ?
			project.getStrtBizDt().toLocalDate().format(formatter) : "");
		data.put("${END_DATE}", project.getEndBizDt() != null ?
			project.getEndBizDt().toLocalDate().format(formatter) : "");
		data.put("${PROJECT_BDGT}", project.getBizBdgt() != null ?
			String.valueOf(project.getBizBdgt()) : "0");
		data.put("${PROJECT_GOAL}", project.getBizGoal() != null ? project.getBizGoal() : "");
		data.put("${PROJECT_DETAIL}", project.getBizDetail() != null ? project.getBizDetail() : "");

		// 첨부파일을 HTML로 변환
		data.put("${PROJECT_FILES}", buildFilesHtml(project.getBizFileId()));

		return data;
	}

	/**
	 * 파일 정보를 HTML로 변환
	 */
	private String buildFilesHtml(String fileId) {
	    if (fileId == null || fileId.isEmpty()) {
	        return "<p style='margin:0; padding:5px;'>첨부파일 없음</p>";
	    }

	    try {
	        // FileMaster로 삭제 여부 확인
	        FileMasterVO fileMaster = fileMasterService.readFileMaster(fileId);
	        if (fileMaster == null || "Y".equals(fileMaster.getDelYn())) {
	            return "<p style='margin:0; padding:5px;'>첨부파일 없음</p>";
	        }

	        // FileDetail 목록 조회
	        List<FileDetailVO> files = fileDetailService.readFileDetailList(fileId);

	        if (files == null || files.isEmpty()) {
	            return "<p style='margin:0; padding:5px;'>첨부파일 없음</p>";
	        }

	        //파일이 존재하면 '원본 파일명(사이즈)'으로 출력
	        StringBuilder html = new StringBuilder();
	        for (FileDetailVO file : files) {
	            html.append("<p style='margin:0; padding:5px;'>📎 ")
	                .append(file.getOrgnFileNm())
	                .append(" (")
	                .append(formatFileSize(file.getFileSize()))
	                .append(")</p>");
	        }
	        return html.toString();

	    } catch (Exception e) {
	        return "<p style='margin:0; padding:5px;'>첨부파일 없음</p>";
	    }
	}

	/**
	 * 파일 크기 포맷 메서드
	 */
	private String formatFileSize(long size) {
		if (size < 1024) return size + " B";
		if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
		return String.format("%.1f MB", size / (1024.0 * 1024.0));
	}

	/**
	 * 내 프로젝트 목록 조회(페이징X)
	 */
	@Override
	public List<ProjectVO> readMyProjectListNonPaging(String userId) {
		return mapper.selectMyProjectListNonPaging(userId);
	}

	/**
	 * 프로젝트 등록(프로젝트 + 참여자)
	 */
	@Override
	@Transactional
	public String createProject(ProjectVO newProject) {
		// 파일이 있을 때만 파일 처리
	    if(newProject.getFileList() != null && !newProject.getFileList().isEmpty()) {
	        fileService.saveFileS3(newProject, FileFolderType.PROJECT.toString());
	    }

		//프로젝트 기본 정보 INSERT
		int rowcnt = mapper.insertProject(newProject);

		if(rowcnt > 0) {
			String bizId = newProject.getBizId(); // selectKey로 생성된 ID

			// 프로젝트 참여자 insert
			List<ProjectMemberVO> members = newProject.getMembers();

			if(members != null && !members.isEmpty()) {
				for(ProjectMemberVO member : members) {
					member.setBizId(bizId); // 생성된 프로젝트 ID 설정
	                projectMemberMapper.insertProjectMember(member);
				}
			}
			return bizId;
		}
		return null;
	}

	/**
	 * 프로젝트 목록 조회(페이징X)
	 */
	@Override
	public List<ProjectVO> readProjectListNonPaging() {
		return mapper.selectProjectListNonPaging();
	}

	/**
	 * 프로젝트 단건 조회
	 */
	@Override
	public ProjectVO readProject(String bizId) {
		ProjectVO project = mapper.selectProject(bizId);
		if(project == null) {
			throw new EntityNotFoundException(project);
		}

		// 멤버 정보 조회해서 추가 (이름 포함)
	    List<ProjectMemberVO> members = projectMemberMapper.selectProjectMemberByProject(bizId);
	    project.setMembers(members);

		return project;
	}

	/**
	 * 프로젝트 수정
	 * @return 성공 시 true, 실패 시 false를 반환
	 */
	@Override
	@Transactional
	public boolean modifyProject(ProjectVO project) {
		// 기존 프로젝트 정보 조회
	    ProjectVO existingProject = mapper.selectProject(project.getBizId());
	    if (existingProject != null) {
	        project.setBizPrgrs(existingProject.getBizPrgrs()); // 기존 진행률 유지
	    }

	    String existingFileId = existingProject.getBizFileId();

	    // ==== 파일 처리 로직 ====

	    // 파일 첨부 여부 플래그
	    boolean hasNewFiles = project.getFileList() != null && !project.getFileList().isEmpty();

	    if(hasNewFiles) {
	        // Case 1: 새 파일 첨부 -> 기존 파일 삭제 처리 및 새 파일 업로드

	        // 기존 파일 Soft Delete
	        if (existingFileId != null && !existingFileId.isEmpty()) {
	            FileMasterVO deleteFm = new FileMasterVO();
	            deleteFm.setFileId(existingFileId);
	            deleteFm.setDelYn("Y");
	            fileMasterService.modifyFileMasterDelyn(deleteFm);
	        }

	        // 새 파일 업로드 -> ProjectVO.bizFileId 갱신됨
	        fileService.saveFileS3(project, FileFolderType.PROJECT.toString());

	    } else {
	        // Case 2: 새 파일 미첨부

	        // 기존 ID를 유지하도록 명시적으로 설정
	        project.setBizFileId(existingFileId);
	    }

	    //프로젝트 기본 정보 업데이트
	    int rowcnt = mapper.updateProject(project);

	    //기본 정보 업데이트가 성공했을 시
	    if(rowcnt > 0) {
	    	List<ProjectMemberVO> newMembers = project.getMembers();

	    	Set<String> newMemberIds = newMembers.stream()
	                .map(ProjectMemberVO::getBizUserId)
	                .collect(Collectors.toSet());

	    	List<ProjectMemberVO> existingMembers = projectMemberMapper.selectProjectMemberByProject(project.getBizId());

	    	//제거된 멤버 Soft Delete 처리
	        for(ProjectMemberVO existingMember : existingMembers) {
	            if (!newMemberIds.contains(existingMember.getBizUserId())) {
	                // UI에서 제외되었으므로, Soft Delete 처리 (B104 업데이트)
	                projectMemberMapper.deleteProjectMember(existingMember);
	            }
	        }

	        //  신규-기존 멤버 분리 및 처리 (UPSERT)
	        if(newMembers != null && !newMembers.isEmpty()) {
	            for(ProjectMemberVO member : newMembers) {
	                member.setBizId(project.getBizId());

	                // 중복 체크 쿼리 (PK 존재 여부만 체크)
	                int count = projectMemberMapper.countProjectMember(member);

	                if(count > 0) {
	                    // 이미 존재하는 멤버는 UPDATE (권한 수정 및 Soft Delete 상태 복구)
	                    projectMemberMapper.updateProjectMember(member);
	                } else {
	                    // 신규 멤버는 INSERT
	                    projectMemberMapper.insertProjectMember(member);
	                }
	            }
	        }
	        return true;
	    }
	    return false;
	}

	/**
	 * 프로젝트 취소(삭제)
	 */
	@Override
	public boolean removeProject(String bizId) {
		int rowcnt = mapper.deleteProject(bizId);
		if(rowcnt == 0) {
			return false;
		}
		return true;
	}


	/**
	 * 프로젝트 목록 조회(페이징 O)
	 */
	@Override
	public List<ProjectVO> readProjectList(PaginationInfo<ProjectVO> paging) {
		int totalRecord = mapper.selectTotalRecord(paging);
		paging.setTotalRecord(totalRecord);

		return mapper.selectProjectList(paging);
	}

	/**
	 * 내 프로젝트 목록 조회(페이징O)
	 */
	@Override
	public List<ProjectVO> readMyProjectList(String userId, PaginationInfo<ProjectVO> paging) {
		// Mapper로 전달할 파라미터 Map 준비 (userId와 paging 정보를 담음)
	    Map<String, Object> paramMap = new HashMap<>();
	    paramMap.put("userId", userId);
	    paramMap.put("paging", paging);

	    // 총 항목 수 조회 (paramMap 사용)
	    //    이 시점에서 userId 조건이 적용
	    int totalRecord = mapper.selectMyProjectTotalRecord(paramMap);

	    // PaginationInfo 객체에 총 항목 수 설정
	    paging.setTotalRecord(totalRecord);

	    // 목록 조회 및 반환 (paramMap 사용)
	    if (totalRecord > 0) {
	        // userId 조건과 페이징 범위가 적용된 목록을 조회
	        return mapper.selectMyProjectList(paramMap);
	    }

	    // 데이터가 없으면 빈 리스트를 반환
	    return Collections.emptyList();
	}

	/**
	 * '진행한 프로젝트' 목록 조회(페이징O)
	 */
	@Override
	public List<ProjectVO> readMyCompletedProjectList(String userId, PaginationInfo<ProjectVO> paging) {
		// Mapper로 전달할 파라미터 Map 준비 (userId와 paging 정보를 담음)
	    Map<String, Object> paramMap = new HashMap<>();
	    paramMap.put("userId", userId);
	    paramMap.put("paging", paging);

	    // 총 항목 수 조회 (paramMap 사용)
	    //    이 시점에서 userId 조건이 적용
	    int totalRecord = mapper.selectMyCompletedProjectTotalRecord(paramMap);

	    // PaginationInfo 객체에 총 항목 수 설정
	    paging.setTotalRecord(totalRecord);

	    // 목록 조회 및 반환 (paramMap 사용)
	    if (totalRecord > 0) {
	        // userId 조건과 페이징 범위가 적용된 목록을 조회
	        return mapper.selectMyCompletedProjectList(paramMap);
	    }

	    // 데이터가 없으면 빈 리스트를 반환
	    return Collections.emptyList();
	}

	/**
	 * 프로젝트 상태를 '취소'->'진행'
	 */
	@Override
	public boolean restoreProject(String bizId) {
	    // 1. 프로젝트 조회
	    ProjectVO project = mapper.selectProject(bizId);

	    if (project == null) {
	        throw new EntityNotFoundException("프로젝트를 찾을 수 없습니다.");
	    }

	    // 2. 취소 상태 체크 - trim() 추가하여 공백 제거
	    String statusCode = project.getBizSttsCd().trim();

	    if (!"취소".equals(statusCode)) {
	        throw new IllegalStateException("취소 상태가 아닌 프로젝트는 복원할 수 없습니다. 현재 상태: " + statusCode);
	    }

	    // 3. 상태를 B302(진행)로 업데이트
	    ProjectVO updateVO = new ProjectVO();
	    updateVO.setBizId(bizId);
	    updateVO.setBizSttsCd("B302");

	    int result = mapper.updateProjectStatus(updateVO);
	    return result > 0;
	}

}
