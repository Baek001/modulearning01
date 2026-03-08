package kr.or.ddit.document.users.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.comm.exception.EntityNotFoundException;
import kr.or.ddit.document.users.service.DocumentUserFileFolderService;
import kr.or.ddit.document.users.service.DocumentUserFileService;
import kr.or.ddit.mybatis.mapper.UserFileFolderMapper;
import kr.or.ddit.mybatis.mapper.UserFileMappingMapper;
import kr.or.ddit.vo.UserFileFolderVO;
import kr.or.ddit.vo.UserFileMappingVO;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author 임가영
 * @since 2025. 10. 15.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 15.     	장어진	          최초 생성
 *  2025. 10. 15.		임가영			readPathView(최상위 폴더부터 현재폴더까지 정보) 서비스 추가
 *  2025. 10. 16.		임가영			폴더 한 건 조회 서비스 추가
 *  2025. 10. 19.		임가영			삭제된 폴더 리스트 가져오는 서비스 추가
 * </pre>
 */
@Service
@RequiredArgsConstructor
public class DocumentUserFileFolderServiceImpl implements DocumentUserFileFolderService{

	private final UserFileFolderMapper mapper;
	private final DocumentUserFileService ufmService;
	private final UserFileMappingMapper ufmMapper;

	@Override
	public List<UserFileFolderVO> retrieveFolderList(Integer upFolderSqn) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		String username = authentication.getName();
		
		return mapper.selectFolderList(upFolderSqn, username);
	}

	@Override
	public List<UserFileFolderVO> retrieveAllFolderList(String userId) {
		return mapper.selectAllFolderList(userId);
	}

	@Override
	public boolean createFolder(UserFileFolderVO uffVO) {
		return mapper.insertFolderList(uffVO) > 0;
	}

	@Override
	public boolean createDefaultFolder(String userId) {
		UserFileFolderVO defaultFolder = new UserFileFolderVO();
		defaultFolder.setUserId(userId);
		defaultFolder.setFolderNm("개인 문서함");
		defaultFolder.setFolderType("DEFAULT"); // 기본 폴더임을 식별하는 타입
		return mapper.insertFolderList(defaultFolder) > 0;
	}

	@Override
	public boolean modifyFolderName(UserFileFolderVO uffVO) {
		UserFileFolderVO folder = mapper.selectFolder(uffVO.getFolderSqn());
		if("DEFAULT".equals(folder.getFolderType())) return false;

		return mapper.updateFolderName(uffVO) > 0;
	}

	@Override
	public boolean moveFolder(UserFileFolderVO uffVO) {
//		UserFileFolderVO folder = mapper.selectFolder(uffVO.getFolderSqn());
		return mapper.updateFolderParent(uffVO) > 0;
	}

	/**
	 * 폴더 삭제 (재귀적으로 하위 폴더와 파일까지 삭제)
	 */
	@Override
	@Transactional
	public boolean removeFolder(List<Integer> folderSqns) {
		// 폴더 DEL_YN 을 Null 로 만든 후
		// 해당 폴더 sqn 을 가지고 있는 userFileMapping 파일들을 가져와서 그 파일의 delYn 을 Y로 바꾼다

		// 해당 폴더 sqn 을 상위 폴더 sqn 으로 가지고 있는 폴더들을 가져와서 그 파일의 delYn 을 Y로 바꾼다
		// 이 작업 끝날때까지 계속 반복..
		boolean success = false;
		int rowcnt = 0;
		
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		String username = authentication.getName();

		for(Integer folderSqn : folderSqns) {
			// 현재 폴더 정보 가져오기
			UserFileFolderVO folder = mapper.selectFolder(folderSqn);
			if (folder == null) return false;
			
			// 선택한 폴더 upFolderSqn null 로 만들기
			mapper.updateUserFolderMappingByDelY(folder);
		}
		
		rowcnt = removeFolderLoop(username, folderSqns);
		
		success = (rowcnt > 0);
		return success;
	}
	
	private int removeFolderLoop(String username, List<Integer> folderSqns) {
		int rowcnt = 0;
		
		for(Integer folderSqn : folderSqns) {
			// 하위 폴더 리스트 조회
			List<UserFileFolderVO> subFolderList = mapper.selectFolderList(folderSqn, username);
			
			if (subFolderList != null && subFolderList.size() != 0) {
				// 하위 폴더 각각 재귀 호출로 삭제
			    for (UserFileFolderVO subFolder : subFolderList) {
			    	removeFolderLoop(username, List.of(subFolder.getFolderSqn()));
			    }
			}

			// 현재 파일 리스트 조회
			UserFileMappingVO paramVO = new UserFileMappingVO();
			paramVO.setUserId(username);
			paramVO.setFolderSqn(folderSqn);
			List<UserFileMappingVO> userFileList = ufmMapper.selectUserFileMappingNonPaging(paramVO);

			// 현재 폴더의 파일 리스트 삭제
			for(UserFileMappingVO file : userFileList) {
				ufmService.removeUserFileMappingByFolder(file.getUserFileId(), file.getUserFileSqn());
			}

			// 현재폴더 삭제
			rowcnt += mapper.deleteFolder(folderSqn);
		}
		
		return rowcnt;
	}

	/**
	 * 최상위폴더부터 현재폴더까지의 이름과 sqn 가져오기
	 */
	@Override
	public List<UserFileFolderVO> readPathView(Integer folderSqn) {
		List<UserFileFolderVO> pathViewList = new ArrayList<>();

		// 현재 폴더 정보 얻어오기
		UserFileFolderVO ufFVO = mapper.selectFolder(folderSqn);
		// 현재 폴더의 상위 폴더 sqn
		Integer uqFolderSqn = ufFVO.getUpFolderSqn();

		pathViewList.add(ufFVO);
		if(uqFolderSqn == null) {
			// 상위 폴더가 null 이라면 최상위 폴더 직전이므로 return
			return pathViewList;
		}

		while (true) {
			// 현재 폴더 정보 얻어오기
			UserFileFolderVO upFolderVO = mapper.selectFolder(uqFolderSqn);
			pathViewList.addFirst(upFolderVO);

			// 현재 폴더의 상위 sqn 이 null 이라면 최상위 폴더 직전이므로 return
			if (upFolderVO.getUpFolderSqn() == null) {
				break;
			}

			uqFolderSqn = upFolderVO.getUpFolderSqn();
		}

		return pathViewList;
	}

	/**
	 * 폴더 하나 상세조회
	 */
	@Override
	public UserFileFolderVO readFileFolder(Integer folderSqn) {
		UserFileFolderVO folderVO = mapper.selectFolder(folderSqn);

		if(folderVO == null) {
			throw new EntityNotFoundException(folderSqn);
		}

		return folderVO;
	}

	/**
	 * 삭제된 폴더에 속한 모든 하위 폴더 목록을 조회 (nonPaging)
	 */
	@Override
	public List<UserFileFolderVO> readUserFileFolderNonPagingByDelY(Integer upFolderSqn) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		String username = authentication.getName();
		
		return mapper.selectUserFileFolderNonPagingByDelY(upFolderSqn, username);
	}

}
