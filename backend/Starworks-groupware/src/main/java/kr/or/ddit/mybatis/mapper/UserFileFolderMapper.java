package kr.or.ddit.mybatis.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
 *
 * </pre>
 */
@Mapper
public interface UserFileFolderMapper {

	/**
	 * 사용자 Id와 상위 폴더에 속한 폴더들을 조회
	 * @param userId
	 * @param upFolderSqn
	 * @return
	 */
	public List<UserFileFolderVO> selectFolderList(@Param("upFolderSqn") Integer upFolderSqn, @Param("userId") String userId);

	/**
	 * 사용자 ID에 속한 모든 폴더들을 조회
	 * @param userId
	 * @return
	 */
	public List<UserFileFolderVO> selectAllFolderList(String userId);

	/**
	 * 폴더 단건 조회
	 * @param folderSqn
	 * @return
	 */
	public UserFileFolderVO selectFolder(Integer folderSqn);

	/**
	 * 폴더 생성
	 * @param vo
	 * @return
	 */
	public int insertFolderList(UserFileFolderVO vo);

	/**
	 * 폴더 이름 수정
	 * @param vo
	 * @return
	 */
	public int updateFolderName(UserFileFolderVO vo);

	/**
	 * 폴더 이동
	 * @param vo
	 * @return
	 */
	public int updateFolderParent(UserFileFolderVO vo);

	/**
	 * 폴더 삭제(soft delete)
	 * @param folderSqn
	 * @return
	 */
	public int deleteFolder(Integer folderSqn);
	
	/**
	 * 삭제된 폴더에 속한 모든 하위 폴더 목록을 조회 (nonPaging)
	 * @param upFolderSqn 현재폴더 sqn
	 * @return
	 */
	public List<UserFileFolderVO> selectUserFileFolderNonPagingByDelY(@Param("upFolderSqn") Integer upFolderSqn, @Param("userId") String userId);
	
	/**
	 * 복구한 폴더 delYn N 로 만들기
	 * @param ufmVO 복구할 폴더sqn 을 담은 vo
	 * @return 성공한 레코드 수
	 */
	public int restoreUserFileFolder(UserFileFolderVO uffVO);
	
	/**
	 * 복구한 폴더 upFolderSqn Null 로 만들기
	 * @param uffVO 복구할 폴더sqn 을 담은 vo
	 * @return 성공한 레코드 수
	 */
	public int updateUserFolderMappingByDelY(UserFileFolderVO uffVO);
}
