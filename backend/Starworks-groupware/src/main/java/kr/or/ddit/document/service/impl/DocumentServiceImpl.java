package kr.or.ddit.document.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.comm.file.service.FileDetailService;
import kr.or.ddit.document.service.DocumentService;
import kr.or.ddit.document.users.service.DocumentUserFileFolderService;
import kr.or.ddit.mybatis.mapper.FileDetailMapper;
import kr.or.ddit.mybatis.mapper.UserFileFolderMapper;
import kr.or.ddit.mybatis.mapper.UserFileMappingMapper;
import kr.or.ddit.vo.FileDetailVO;
import kr.or.ddit.vo.UserFileFolderVO;
import kr.or.ddit.vo.UserFileMappingVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author 임가영
 * @since 2025. 10. 19.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 19.     	임가영	          최초 생성
 *
 * </pre>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentServiceImpl implements DocumentService {
	
	private final FileDetailService fileDetailService;
	private final DocumentUserFileFolderService userFolderService;
	private final FileDetailMapper fileDetailMapper;
	private final UserFileMappingMapper userFileMapper;
	private final UserFileFolderMapper userFolderMapper;

	/**
	 * 자료실 파일을 복구하는 서비스
	 */
	@Override
	@Transactional
	public boolean restoreDocumentFile(List<String> fileIds, List<Integer> fileSeqs) {
		boolean success = false;
		int rowcnt = 0;
		
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		String username = authentication.getName();
		
		for(int i = 0; i < fileIds.size(); i++) {
			String fileId = fileIds.get(i);
			Integer fileSeq = fileSeqs.get(i);
			
			// fileDetail 테이블에서 복원
			FileDetailVO fdVO = new FileDetailVO();
			fdVO.setFileId(fileId);
			fdVO.setFileSeq(fileSeq);
			rowcnt += fileDetailMapper.restoreFileDetail(fdVO);
			
			// userFileMapping 테이블에서 FolderSqn null로 변경
			UserFileMappingVO uffVO1 = new UserFileMappingVO();
			uffVO1.setUserId(username);
			uffVO1.setUserFileId(fileId);
			uffVO1.setUserFileSqn(fileSeq);
			uffVO1 = userFileMapper.selectUserFileMapping(uffVO1);
			
			if (uffVO1 != null) {
				userFileMapper.updateUserFileMappingByDelY(uffVO1);
			}
		}
		
		if (rowcnt == fileIds.size()) success = true;
		
		return success;
	}
	
	/**
	 * 자료실 폴더를 복구하는 서비스
	 */
	@Override
	@Transactional
	public boolean restoreDocumentFolder(List<Integer> folderSqns) {
		boolean success = false;
		int rowcnt = 0;
		
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		String username = authentication.getName();
		
		try {
			// 선택한 폴더 upFolderSqn null 로 만들기
			for(Integer folderSqn : folderSqns) {
				UserFileFolderVO userFolderVO = new UserFileFolderVO();
				userFolderVO.setFolderSqn(folderSqn);
				userFolderMapper.updateUserFolderMappingByDelY(userFolderVO);
			}
			
			rowcnt = restoreDocumentLoop(username, folderSqns);
			
			success = (rowcnt > 0);
			return success;
			
		} catch (Exception e) {
			log.error("문서 복구 중 오류 발생: {}", e.getMessage(), e);
			throw new RuntimeException("복구실패", e);
		}
	}
	
	// 하위폴더까지 복구하는 재귀함수
	private int restoreDocumentLoop(String username, List<Integer> folderSqns) {
		int rowcnt = 0;
		
		for(Integer folderSqn : folderSqns) {
			// 현재 폴더 del Y => N 으로 변경
			UserFileFolderVO uffVO = new UserFileFolderVO();
			uffVO.setFolderSqn(folderSqn);
			int nullChangeSuccess = userFolderMapper.restoreUserFileFolder(uffVO);
			
			// 현재 폴더에 속한 파일 전부 조회
			UserFileMappingVO ufmVO = new UserFileMappingVO();
			ufmVO.setUserId(username);
			ufmVO.setFolderSqn(folderSqn);
			List<UserFileMappingVO> fileList = userFileMapper.selectUserFileMappingNonPagingByDelY(ufmVO);
			
			// 현재 폴더에 속한 파일이 있다면
			if (fileList != null && fileList.size() != 0) {
				for(UserFileMappingVO file : fileList) {
					String fileId = file.getUserFileId();
					Integer fileSeq = file.getUserFileSqn();
					
					// fileDetail 테이블에서 복원
					FileDetailVO fdVO = new FileDetailVO();
					fdVO.setFileId(fileId);
					fdVO.setFileSeq(fileSeq);
					rowcnt += fileDetailMapper.restoreFileDetail(fdVO);
				}
				
				List<Integer> downFolderSqnList = new ArrayList<>();
				// 하위 폴더 리스트 가져오기
				List<UserFileFolderVO> downFolderList = userFolderMapper.selectUserFileFolderNonPagingByDelY(folderSqn, username);
				if (downFolderList.size() != 0) {
					// 폴더sqn 리스트를 하위 폴더로 설정
					for(UserFileFolderVO downFolder : downFolderList) {
						downFolderSqnList.add(downFolder.getFolderSqn());
					}
					restoreDocumentLoop(username, downFolderSqnList);
				}
			} else {
				rowcnt = 1;
			}
		}
		return rowcnt;
	}

	/**
	 * 현재 폴더 및 하위 폴더의 모든 파일 정보를 재귀적으로 조회하는 서비스 (zip 파일 변환을 위한)
	 */
	@Override
	public List<Map<String, Object>> readAllFilesInFolderRecursive(Integer folderSqn) {
		List<Map<String, Object>> zipFileList = new ArrayList<>();
		
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		String username = authentication.getName();
		
		// 현재 폴더의 정보 가져오기
		String rootPath = "";
		filesInFolderRecursive(username, folderSqn, rootPath, zipFileList);
		
		return zipFileList;
	}
	
	private void filesInFolderRecursive(String username, Integer folderSqn, String currentPath, List<Map<String, Object>> zipFileList) {
		// 현재 폴더의 정보 가져오기
		UserFileFolderVO currentFolder = userFolderMapper.selectFolder(folderSqn);
		String newPath = currentPath.isEmpty() ? currentFolder.getFolderNm() : currentPath + "/" + currentFolder.getFolderNm();
		 
		// 현재 폴더의 파일리스트 가져오기
		UserFileMappingVO ufmVO = new UserFileMappingVO();
		ufmVO.setUserId(username);
		ufmVO.setFolderSqn(currentFolder.getFolderSqn());
		List<UserFileMappingVO> currentFileList = userFileMapper.selectUserFileMappingNonPaging(ufmVO);
		
		if(currentFileList != null && currentFileList.size() != 0) {
			// 현재 폴더의 파일리스트가 있으면
			for(UserFileMappingVO currentFile : currentFileList) {
				Map<String, Object> paramMap = fileDetailService.readFileDetailS3(currentFile.getFileDetailVO().getSaveFileNm());
				List<UserFileFolderVO> pathList = userFolderService.readPathView(currentFile.getFolderSqn());
				paramMap.put("folderPath", newPath);
				
				zipFileList.add(paramMap);
			}
		}
		
		// 현재폴더의 하위 폴더들 가져오기
		List<UserFileFolderVO> currentFolderList = userFolderMapper.selectFolderList(folderSqn, username);
		if(currentFolderList != null && currentFolderList.size() != 0) {
			for(UserFileFolderVO currentFolder2 : currentFolderList) {
				filesInFolderRecursive(username, currentFolder2.getFolderSqn(), newPath, zipFileList);
			}
		}
	}
}
