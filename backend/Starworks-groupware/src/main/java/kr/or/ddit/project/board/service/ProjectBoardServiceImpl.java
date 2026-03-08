package kr.or.ddit.project.board.service;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import kr.or.ddit.comm.exception.EntityNotFoundException;
import kr.or.ddit.comm.file.FileFolderType;
import kr.or.ddit.comm.file.service.impl.FileDeleteServiceImpl;
import kr.or.ddit.comm.file.service.impl.FileUploadServiceImpl;
import kr.or.ddit.comm.paging.PaginationInfo;
import kr.or.ddit.mybatis.mapper.FileMasterMapper;
import kr.or.ddit.mybatis.mapper.ProjectBoardCommentMapper;
import kr.or.ddit.mybatis.mapper.ProjectBoardMapper;
import kr.or.ddit.vo.FileMasterVO;
import kr.or.ddit.vo.ProjectBoardCommentVO;
import kr.or.ddit.vo.ProjectBoardVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author 장어진
 * @since 2025. 9. 25.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 25.     	장어진	          최초 생성
 *  2025. 9. 25.     	장어진	          최초 생성
 *  2025.10. 02.     	장어진	          기능 제작을 위한 각종 코드 추가
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectBoardServiceImpl implements ProjectBoardService{
	
	private final ProjectBoardMapper mapper;
	private final FileMasterMapper fmMapper;
	private final ProjectBoardCommentMapper pbcMapper; 
	
	private final FileUploadServiceImpl fUpService;
	private final FileDeleteServiceImpl fDelService;
	
	@Value("${file-info.file.path}")
	private Resource fileFilderRes;
	@Value("${file-info.file.path}")
	private File fileFolder;
	
	/**
	 * 조회수 증가
	 */
	@Override
	public boolean modifyViewCnt(String bizPstId) {
		return mapper.updateViewCnt(bizPstId) > 0;
	}	

	/**
	 * 프로젝트 게시물 개수 조회
	 */
	@Override
	public int readProjectBoardTotalRecord(PaginationInfo<ProjectBoardVO> paging, String bizId) {
		Map<String, Object> paramMap = new HashMap<>();
		paramMap.put("paging", paging);
		paramMap.put("bizId", bizId);
		return mapper.selectProjectBoardTotalRecord(paramMap);
	}	

	/**
	 * 프로젝트 게시물 목록 조회 (페이징 X)
	 */
	@Override
	public List<ProjectBoardVO> readProjectBoardListNonPaging(String bizId) {
		return mapper.selectProjectBoardListNonPaging(bizId);
	}

	/**
	 * 프로젝트 게시물 목록 조회 (페이징 O)
	 */
	@Override
	public List<ProjectBoardVO> readProjectBoardListPaging(PaginationInfo<ProjectBoardVO> paging, String bizId) {
		Map<String, Object> paramMap = new HashMap<>(); 
		paramMap.put("paging", paging);
		paramMap.put("bizId", bizId);

		int totalRecord = mapper.selectProjectBoardTotalRecord(paramMap);
		paging.setTotalRecord(totalRecord);

		paramMap.put("paging", paging);
		return mapper.selectProjectBoardListPaging(paramMap);
	}	
	
	/**
	 * 프로젝트 게시물 단건 조회
	 */
	@Override
	public ProjectBoardVO readProjectBoard(String bizPstId) {
		ProjectBoardVO vo = mapper.selectProjectBoard(bizPstId);
		if (vo == null) {
			throw new EntityNotFoundException(bizPstId);
		}
		return vo;
	}

	@Override
	public boolean createProjectBoard(ProjectBoardVO pb) {
	    if(pb.getFileList() != null && pb.getFileList().size() != 0) {
	        fUpService.saveFileS3(pb, FileFolderType.PROJECT_BOARD.toString());
	    }
		return mapper.insertProjectBoard(pb) > 0;
	}

	@Override
	public boolean modifyProjectBoard(ProjectBoardVO pb) {
		// 만약 첨부파일이 있으면 기존 프로젝트 게시글 파일 Id 가져와서 삭제여부 Y 로 변경
		if(pb.getFileList() != null && pb.getFileList().size() != 0) {
			ProjectBoardVO existBoard = mapper.selectProjectBoard(pb.getBizPstId());
			
			if(existBoard.getBizPstFileId() != null && !existBoard.getBizPstFileId().isEmpty()) {
				FileMasterVO fileMaster = new FileMasterVO();
				fileMaster.setFileId(existBoard.getBizPstFileId());
				fmMapper.updateFileMasterDelyn(fileMaster);
			}
			fUpService.saveFileS3(pb, FileFolderType.PROJECT_BOARD.toString());		
		}
		return mapper.updateProjectBoard(pb) > 0;
	}

	@Override
	public boolean removeProjectBoard(String bizPstId) {
		// 기존 게시글 파일 Id 가져와서 만약 있으면 삭제여부 Y로 변경 
		ProjectBoardVO existBoard = mapper.selectProjectBoard(bizPstId);
		String fileId = existBoard.getBizPstFileId();
		fDelService.deleteFileDB(fileId);	
		
	    List<ProjectBoardCommentVO> commentList = pbcMapper.selectProjectBoardCommentList(bizPstId);
	    for(ProjectBoardCommentVO comment : commentList) {
	        pbcMapper.deleteProjectBoardComment(comment.getBizCmntId());
	    }		
		
		return mapper.deleteProjectBoard(bizPstId) > 0;
	}
}
