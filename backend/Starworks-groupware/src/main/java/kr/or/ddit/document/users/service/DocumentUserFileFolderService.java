package kr.or.ddit.document.users.service;

import java.util.List;

import kr.or.ddit.vo.UserFileFolderVO;
import kr.or.ddit.vo.UserFileMappingVO;

/**
 *
 * @author 장어진
 * @since 2025. 10. 10.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 10.     	장어진	          최초 생성
 *  2025. 10. 15.		임가영			readPathView(최상위 폴더부터 현재폴더까지 정보) 서비스 추가
 *  2025. 10. 16.		임가영			폴더 한 건 조회 서비스 추가
 *  2025. 10. 19.		임가영			삭제된 폴더 리스트 가져오는 서비스 추가
 * </pre>
 */
public interface DocumentUserFileFolderService {
	/**
	 * 폴더에 속한 모든 하위 폴더 목록을 조회 (nonPaging)
	 * @param upFolderSqn
	 * @param userId
	 * @return
	 */
	public List<UserFileFolderVO> retrieveFolderList(Integer upFolderSqn);

	/**
	 * 사용자에 속한 모든 폴더 목록 조회
	 * @param userId
	 * @return
	 */
	public List<UserFileFolderVO> retrieveAllFolderList(String userId);

	/**
	 * 새 폴더 생성
	 * @param uffVO
	 * @return
	 */
	public boolean createFolder(UserFileFolderVO uffVO);

	/**
	 * 사용자의 최상위 기본 폴더를 생성
	 * @param userId
	 * @return
	 */
	public boolean createDefaultFolder(String userId);

	/**
	 * 폴더 이름 변경
	 * @param uffVO
	 * @return
	 */
	public boolean modifyFolderName(UserFileFolderVO uffVO);

	/**
	 * 폴더 이동
	 * @param uffVO
	 * @return
	 */
	public boolean moveFolder(UserFileFolderVO uffVO);

	/**
	 * 폴더 삭제 (soft delete)
	 * @param folderSqn
	 * @return
	 */
	public boolean removeFolder(List<Integer> folderSqns);

	/**
	 * 최상위폴더부터 현재폴더까지의 이름과 sqn 가져오기
	 * @param folderSqn 현재 폴더 sqn
	 * @return
	 */
	public List<UserFileFolderVO> readPathView(Integer folderSqn);

	/**
	 * 폴더 하나 상세 조회
	 * @param folderSqn 폴더 sqn
	 * @return 조회 결과 없으면 EntityNotFoundExcetion 발생
	 */
	public UserFileFolderVO readFileFolder(Integer folderSqn);
	
	/**
	 * 삭제된 폴더에 속한 모든 하위 폴더 목록을 조회 (nonPaging)
	 * @param upFolderSqn 현재폴더 sqn
	 * @return
	 */
	public List<UserFileFolderVO> readUserFileFolderNonPagingByDelY(Integer upFolderSqn);
}
