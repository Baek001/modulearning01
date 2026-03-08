package kr.or.ddit.mybatis.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
 *  2025. 10. 5.		임가영			  insertUserFileMapping Mapper 추가
 *  2025. 10. 8.		장어진			  파일 목록 조회 paging으로 변경
 *  2025. 10. 10.		장어진			  폴더 기능을 위한 update Mapper 추가
 *  2025. 10. 13.		임가영			  nonPaging 메소드 추가
 *  2025. 10. 17.		임가영			  삭제된 파일 전부 가져오는 Mapper 추가
 *  2025. 10. 19.		임가영			  복구된 파일 FolderSqn null 로 만드는 Mapper 추가
 * </pre>
 */
@Mapper
public interface UserFileMappingMapper {

	/**
	 * 개인 파일 목록 조회(Paging).
	 * @param paramMap
	 * @return 조회 결과 없으면 list.size() == 0
	 */
	public List<UserFileMappingVO> selectUserFileMappingListPaging(Map<String, Object> paramMap);

	/**
	 *
	 * @param folderSqn
	 * @return
	 */
	public List<UserFileMappingVO> selectUserFileMappingInFolder(Integer folderSqn);

	/**
	 * 페이징을 위한 전체 개인 파일 개수 조회
	 * @param paramMap
	 * @return 파일이 존재하지 않으면 0
	 */
	public int selectUserFileMappingTotalRecord(Map<String, Object> paramMap);

	/**
	 * 개인 파일 단건 조회
	 * @param userMapping 사용자Id, 파일Id, 파일sqn 을 담은 vo
	 * @return 조회 결과 없을 시 null
	 */
	public UserFileMappingVO selectUserFileMapping(UserFileMappingVO userMapping);

	/**
	 * 개인문서함 데이터 삽입
	 * @param userMapping 문서함 파일 Id, 사용자 Id 를 담은 vo
	 * @return 성공한 레코드 수
	 */
	public int insertUserFileMapping(UserFileMappingVO userMapping);

	/**
	 * 파일의 소속 폴더 변경
	 * @param paramMap
	 * @return 성공한 레코드 수
	 */
	public int updateFileFolder(UserFileMappingVO userMapping);

	/**
	 * 개인문서함 목록 조회 (페이징 X)
	 * @param userId
	 * @return
	 */
	public List<UserFileMappingVO> selectUserFileMappingNonPaging(UserFileMappingVO ufmVO);

	/**
	 * 개인문서함 목록 조회 (페이징 O)
	 * @param paramMap UserFIleMappingVO 와 Paging 이 들어있는 Map
	 * @return
	 */
	public List<UserFileMappingVO> selectUserFileMappingList(Map<String, Object> paramMap);
	
	/**
	 * 개인문서함 삭제된 파일 전부 가져오기
	 * @param ufmVO 유저 Id 가 담긴 vo
	 * @return
	 */
	public List<UserFileMappingVO> selectUserFileMappingNonPagingByDelY(UserFileMappingVO ufmVO);
	
	/**
	 * 개인문서함 복구한 파일 FolderSqn null 로 만들기
	 * @param ufmVO
	 * @return
	 */
	public int updateUserFileMappingByDelY(UserFileMappingVO ufmVO);
}
