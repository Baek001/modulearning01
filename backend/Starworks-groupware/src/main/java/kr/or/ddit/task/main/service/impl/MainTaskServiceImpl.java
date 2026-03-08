package kr.or.ddit.task.main.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import kr.or.ddit.comm.exception.EntityNotFoundException;
import kr.or.ddit.comm.file.FileFolderType;
import kr.or.ddit.comm.file.service.impl.FileUploadServiceImpl;
import kr.or.ddit.comm.paging.PaginationInfo;
import kr.or.ddit.mybatis.mapper.MainTaskMapper;
import kr.or.ddit.mybatis.mapper.ProjectMapper;
import kr.or.ddit.task.main.service.MainTaskService;
import kr.or.ddit.vo.MainTaskVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author 김주민
 * @since 2025. 9. 26.
 * @see MainTaskServiceImpl
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 26.     	김주민	          최초 생성
 *  2025. 10. 06. 		김주민			FileUploadServie 주입
 *  2025. 10. 08. 		김주민			updateTaskStatus 추가
 *
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MainTaskServiceImpl implements MainTaskService{

	private final MainTaskMapper mapper;
	private final FileUploadServiceImpl fileService;
	private final ProjectMapper projectMapper;

	/**
	 * 업무 상태 변경
	 */
	@Override
	@Transactional
	public boolean updateTaskStatus(String taskId, String taskSttsCd) {
		// 업무 상태 업데이트
		int rowcnt = mapper.updateTaskStatus(taskId, taskSttsCd);

		if (rowcnt > 0) {
			// 해당 업무가 속한 프로젝트 ID 조회
			MainTaskVO task = mapper.selectMainTask(taskId);
			if (task != null && task.getBizId() != null) {
				// 프로젝트 진행률 자동 재계산
				projectMapper.updateProjectProgress(task.getBizId());
			}
			return true;
		}
		return false;
	}

	/**
	 * 주요 업무 추가
	 */
	@Override
	public boolean createMainTask(MainTaskVO newMainTask) {
		 //첨부파일 리스트를 가져옵니다
        List<MultipartFile> fileList = newMainTask.getFileList();

        //null 체크 및 리스트 비어있는지 체크 추가
        if (fileList != null && !fileList.isEmpty()) {
		    fileService.saveFileS3(newMainTask, FileFolderType.TASK.toString());
        }

		int rowcnt = mapper.insertMainTask(newMainTask);
		if(rowcnt == 0) {
			return false;
		}
		return true;
	}

	/**
	 * 주요 업무 목록 조회
	 */
	@Override
	public List<MainTaskVO> readMainTaskListNonPaging(String bizId) {
		return mapper.selectMainTaskListNonPaging(bizId);
	}

	/**
	 * 주요 업무 단건 조회
	 */
	@Override
	public MainTaskVO readMainTask(String taskId) {
		MainTaskVO mainTask = mapper.selectMainTask(taskId);
		if(mainTask == null) {
			throw new EntityNotFoundException(mainTask);
		}
		return mainTask;
	}

	/**
	 * 주요 업무 수정
	 */
	@Override
	public boolean modifyMainTask(MainTaskVO mainTask) {
		//새로운 첨부파일이 있는 경우에만 파일 업로드 처리
		List<MultipartFile> fileList = mainTask.getFileList();
		if(fileList != null && !fileList.isEmpty()) {
			//새 파일 업로드하고 파일 ID 생성
			fileService.saveFileS3(mainTask, FileFolderType.TASK.toString());
		}
		//fileList가 null이거나 비어있으면 기존 taskFileId 유지

		//업무 정보 업데이트
		int rowcnt = mapper.updateMainTask(mainTask);
		if(rowcnt == 0) {
			return false;
		}
		return true;
	}

	/**
	 * 주요 업무 삭제
	 */
	@Override
	@Transactional
	public boolean removeMainTask(String taskId) {
		// 삭제 전 프로젝트 ID 조회
		MainTaskVO task = mapper.selectMainTask(taskId);
		String bizId = (task != null) ? task.getBizId() : null;

		int rowcnt = mapper.deleteMainTask(taskId);

		if(rowcnt > 0) {
			// 업무 삭제 후 프로젝트 진행률 재계산
			if (bizId != null) {
				projectMapper.updateProjectProgress(bizId);
			}
			return true;
		}
		return false;
	}

	/**
	 * 주요 업무 조회 (페이징O, 담당자 필터링 지원)
	 * @param paging 페이징 정보
	 * @param bizId 프로젝트 ID
	 * @param bizUserId 담당자 ID (선택사항, null이면 전체 조회)
	 * @return 업무 목록
	 */
	@Override
	public List<MainTaskVO> readMainTaskList(PaginationInfo<MainTaskVO> paging, String bizId, String bizUserId) {
		// 전체 레코드 수 조회 (필터 조건 포함)
		int totalRecord = mapper.selectTotalRecord(bizId, bizUserId);
		paging.setTotalRecord(totalRecord);

		// 업무 목록 조회 (필터 조건 포함)
		return mapper.selectMainTaskList(paging, bizId, bizUserId);
	}

	/**
	 * 내 업무 리스트 조회
	 */
	@Override
	public List<MainTaskVO> readMyTaskList(String bizUserId,PaginationInfo<MainTaskVO> paging) {
		int totalRecord = mapper.selectMyTaskTotalRecord(bizUserId);
		paging.setTotalRecord(totalRecord);

		log.info("업무 전체 개수: " + totalRecord); // 디버깅용
		log.info("현재 페이지: " + paging.getCurrentPage());
		log.info("총 페이지 수: " + paging.getTotalPage());

		return mapper.selectMyTaskList(bizUserId,paging);
	}

	/**
	 * 내 업무 리스트 조회 - 대시보드용
	 */
	@Override
	public List<MainTaskVO> readMyTaskListNonPaging(String bizUserId) {
		return mapper.selectMyTaskListNonPaging(bizUserId);
	}

	/**
	 * 상태별 업무 리스트 조회
	 */
	@Override
	public List<MainTaskVO> readMyTaskListByStatus(String userId, String status, PaginationInfo<MainTaskVO> paging) {
		int totalRecord = mapper.selectMyTaskTotalRecordByStatus(userId, status);
	    paging.setTotalRecord(totalRecord);

	    // 상태별 목록 조회
	    return mapper.selectMyTaskListByStatus(userId, status, paging);
	}
}
