package kr.or.ddit.document.users.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ModelAttribute;

import kr.or.ddit.comm.exception.EntityNotFoundException;
import kr.or.ddit.comm.file.FileFolderType;
import kr.or.ddit.comm.file.service.FileDetailService;
import kr.or.ddit.comm.file.service.impl.FileUploadServiceImpl;
import kr.or.ddit.comm.paging.PaginationInfo;
import kr.or.ddit.document.exception.NotAccessFileRemoveException;
import kr.or.ddit.document.users.service.DocumentUserFileService;
import kr.or.ddit.mybatis.mapper.FileDetailMapper;
import kr.or.ddit.mybatis.mapper.FileMasterMapper;
import kr.or.ddit.mybatis.mapper.UserFileMappingMapper;
import kr.or.ddit.vo.FileDetailVO;
import kr.or.ddit.vo.FileMasterVO;
import kr.or.ddit.vo.UserFileMappingVO;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
/**
 *
 * @author 장어진
 * @since 2025. 9. 26.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자             수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 26.     	장어진	        최초 생성
 *  2025.10.  5.		임가영			createUserFileMapping(개인문서함 insert) 메소드 추가
 *  2025.10.  8.		장어진			Paging & simple search 기능 구현을 위한 메소드 추가
 *  2025. 10. 13.		임가영			개인보관함 nonpaging 메소드 추가
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentUserFileServiceImpl implements DocumentUserFileService{

	private final UserFileMappingMapper mapper;
	private final FileUploadServiceImpl fUpService;
	private final FileMasterMapper fmMapper;
	private final FileDetailService fdService;
	private final FileDetailMapper fdMapper;

	@Override
	public int retrieveUserFileMappingTotalRecord(PaginationInfo<UserFileMappingVO> paging, Integer folderSqn) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		String username = authentication.getName();

		Map<String, Object> paramMap = new HashMap<>();
		paramMap.put("paging", paging);
		paramMap.put("userId", username);
		paramMap.put("folderSqn", folderSqn);
		return mapper.selectUserFileMappingTotalRecord(paramMap);
	}

	@Override
	public List<UserFileMappingVO> retrieveUserFileMappingsInFolder(Integer folderSqn) {
		return mapper.selectUserFileMappingInFolder(folderSqn);
	}

	@Override
	public List<UserFileMappingVO> retrieveUserFileMappingListPaging(PaginationInfo<UserFileMappingVO> paging, String userId, Integer folderSqn) {
		Map<String, Object> paramMap = new HashMap<>();
		paramMap.put("paging", paging);
		paramMap.put("userId", userId);
		paramMap.put("folderSqn", folderSqn);

		int totalRecord = mapper.selectUserFileMappingTotalRecord(paramMap);
		paging.setTotalRecord(totalRecord);

		paramMap.put("paging", paging);
		return mapper.selectUserFileMappingListPaging(paramMap);
	}

//	@Override
//	public UserFileMappingVO readUserFileMapping(String userId, String userFileId) {
//		UserFileMappingVO vo = mapper.selectUserFileMapping(userId, userFileId);
//		if (vo == null) {
//			Map<String, Object> paramMap = new HashMap<>();
//			paramMap.put("userId", userId);
//			paramMap.put("userFileId", userFileId);
//			throw new EntityNotFoundException(paramMap);
//		}
//		return vo;
//	}

	/**
	 * 개인 문서함 insert
	 */
	@Override
	@Transactional
	public int createUserFileMapping(@ModelAttribute UserFileMappingVO userMapping) {
		// 현재 폴더 sqn
		Integer folderSqn = userMapping.getFolderSqn();

		// s3에 업로드
		fUpService.saveFileS3(userMapping, FileFolderType.DOCUMENT_USER.toString());

		// 파일 Id 얻어오기
		String fileId = userMapping.getUserFileId();
		if(fileId == null) {
			throw new RuntimeException("파일 저장 후 fileId를 가져오지 못했습니다.");
		}

		log.error("============> fileId: {}", fileId);

		List<FileDetailVO> savedFileList = fdService.readFileDetailList(fileId);

	    if(savedFileList.isEmpty()) {
	        throw new RuntimeException("FILE_DETAIL에 저장된 파일이 없습니다.");
	    }

		int cnt = 0;
		for(FileDetailVO savedFile : savedFileList) {
			log.error("============> savedFile - fileId: {}, fileSeq: {}", savedFile.getFileId(), savedFile.getFileSeq());


			UserFileMappingVO mappingToInsert = new UserFileMappingVO();

			mappingToInsert.setUserId(userMapping.getUserId());
			mappingToInsert.setFolderSqn(folderSqn);
			mappingToInsert.setUserFileId(savedFile.getFileId());
			mappingToInsert.setUserFileSqn(savedFile.getFileSeq());

			cnt += mapper.insertUserFileMapping(mappingToInsert);
		}

		return cnt;
	}

	@Override
	@Transactional
	public boolean removeUserFileMappingByFolder(String fileId, Integer fileSeq) {
		return fdService.removeFileDetail(fileId, fileSeq);
	}

	/**
	 * 자료실 파일 삭제
	 */
	@Override
	@Transactional
	public boolean removeUserFileMappingByFile(List<String> fileIds, List<Integer> fileSeqs) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		String username = authentication.getName();
		
		for(int i = 0; i<fileSeqs.size(); i++) {
			FileMasterVO fmVO = new FileMasterVO();
			
			String id = fileIds.get(i); // 파일리스트에서 fileId 하나씩 꺼내오기
			Integer seq = fileSeqs.get(i); // 파일리스트에서 fileSeq 하나씩 꺼내오기

			fmVO.setCrtUserId(username);
			fmVO.setFileId(id);
			fmVO.setFileSeq(seq);
			if(fmMapper.selectFileMasterByUser(fmVO) == null) {
				throw new NotAccessFileRemoveException("파일 소유자가 아닙니다.");
			}

			UserFileMappingVO ufmVO = new UserFileMappingVO();
			ufmVO.setFileId(id);
			ufmVO.setUserFileSqn(seq);
			// 삭제한 파일 folderSqn 전부 null로 만들기
			mapper.updateUserFileMappingByDelY(ufmVO);
			
			fdService.removeFileDetail(id, seq);
		}
		return true;
	}

	@Override
	public boolean moveUserFile(UserFileMappingVO userMapping) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		userMapping.setUserId(authentication.getName());
		return mapper.updateFileFolder(userMapping) > 0;
	}

	/**
	 * 현재폴더의 파일리스트 얻어오기 (페이징 x)
	 */
	@Override
	public List<UserFileMappingVO> readUserFileMappingListNonPaging(Integer folderSqn) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		String username = authentication.getName();

		UserFileMappingVO ufmVO = new UserFileMappingVO();
		ufmVO.setUserId(username);
		ufmVO.setFolderSqn(folderSqn);

		return mapper.selectUserFileMappingNonPaging(ufmVO);
	}

	/**
	 * 현재폴더의 파일리스트 얻어오기 (페이징 o)
	 */
	@Override
	public List<UserFileMappingVO> readUserFileMappingList(Integer folderSqn, PaginationInfo<UserFileMappingVO> paging) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		String username = authentication.getName();

		UserFileMappingVO ufmVO = new UserFileMappingVO();
		ufmVO.setUserId(username);
		ufmVO.setFolderSqn(folderSqn);

		Map<String, Object> paramMap = new HashMap<>();
		paramMap.put("userFileMappingVO", ufmVO);

		List<UserFileMappingVO> fileList = mapper.selectUserFileMappingNonPaging(ufmVO);

		int totalRecord = fileList.size();
		if (fileList.isEmpty()) totalRecord = 1;
		paging.setTotalRecord(totalRecord);

		paramMap.put("paging", paging);

		return mapper.selectUserFileMappingList(paramMap);
	}

	/**
	 * 현재 폴더의 삭제된 파일 전부 가져오기
	 */
	@Override
	public List<UserFileMappingVO> readUserFileMappingNonPagingByDelY(Integer folderSqn) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		String username = authentication.getName();

		UserFileMappingVO ufmVO = new UserFileMappingVO();
		ufmVO.setUserId(username);
		ufmVO.setFolderSqn(folderSqn);

		return mapper.selectUserFileMappingNonPagingByDelY(ufmVO);
	}
}
