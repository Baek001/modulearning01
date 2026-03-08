package kr.or.ddit.document.users.service;

import java.util.List;

import kr.or.ddit.comm.paging.PaginationInfo;
import kr.or.ddit.vo.UserFileMappingVO;

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
 *  2025. 9. 26.     	장어진	          주석 내용 추가
 *	2025.10.  5.		임가영			createUserFileMapping 메소드 추가
 *  2025.10.  8.		장어진			Paging & simple search 기능 구현을 위한 메소드 추가
 *  2025. 10. 17.		임가영			  삭제된 파일 전부 가져오는 Mapper 추가
 * </pre>
 */
public interface DocumentUserFileService {

	/**
	 * 폴더에 속한 개인 파일 목록 조회(Paging)
	 * @param userId  : 사용자 ID
	 * @param folderSqn : 폴더 시퀀스 번호
	 * @param paging
	 * @return 조회 결과 없으면 list.size() == 0
	 */
	public List<UserFileMappingVO> retrieveUserFileMappingListPaging(PaginationInfo<UserFileMappingVO> paging, String userId, Integer folderSqn);

	/**
	 * 페이징을 위한 폴더에 속한 파일 갯수
	 * @param paging
	 * @param folderSqn : 폴더 시퀀스 번호
	 * @return 조회 결과 없으면 0
	 */
	public int retrieveUserFileMappingTotalRecord(PaginationInfo<UserFileMappingVO> paging, Integer folderSqn);

	/**
	 * 특정 폴더에 속한 모든 파일 매핑 정보 조회
	 * @param folderSqn
	 * @return
	 */
	public List<UserFileMappingVO> retrieveUserFileMappingsInFolder(Integer folderSqn);

	/**
	 * 개인 파일 단건 조회
	 * @param userId : 사용자 ID
	 * @param userFileId : 사용자와 매핑된 File ID
	 * @return 조회 결과 없으면 EntityNotFoundException
	 */
//	public UserFileMappingVO readUserFileMapping(String userId, String userFileId);

	/**
	 * 개인문서함 데이터 등록
	 * @param userMapping 문서함 파일 Id, 사용자 Id 를 담은 vo
	 * @return 성공한 레코드 수
	 */
	public int createUserFileMapping(UserFileMappingVO userMapping);

	/**
	 * 파일 이동
	 * @param userMapping
	 * @return 성공 시 true, 실패 시 false
	 */
	public boolean moveUserFile(UserFileMappingVO userMapping);

	/**
	 * 파일 삭제 (soft delete)
	 * @param fileId : 파일 ID
	 * @param fileSeq : 파일 시퀀스 번호
	 * @return 성공 시 true, 실패 시 false
	 */
	public boolean removeUserFileMappingByFolder(String fileId, Integer fileSeq);

	/**
	 * 파일 삭제 (soft delete)
	 * @param fileId : 파일 ID
	 * @param fileSeq : 파일 시퀀스 번호
	 * @return 성공 시 true, 실패 시 false
	 */
	public boolean removeUserFileMappingByFile(List<String> fileIds, List<Integer> fileSeqs);

	/**
	 * 개인문서함 목록 조회 (페이징 X)
	 * @return
	 */
	public List<UserFileMappingVO> readUserFileMappingListNonPaging(Integer folderSqn);

	/**
	 * 개인문서함 목록 조회 (페이징 O)
	 * @return
	 */
	public List<UserFileMappingVO> readUserFileMappingList(Integer folderSqn, PaginationInfo<UserFileMappingVO> paging);

	/**
	 * 개인문서함 삭제된 파일 전부 가져오기
	 * @param 현재폴더 sqn
	 * @return
	 */
	public List<UserFileMappingVO> readUserFileMappingNonPagingByDelY(Integer folderSqn);

}
