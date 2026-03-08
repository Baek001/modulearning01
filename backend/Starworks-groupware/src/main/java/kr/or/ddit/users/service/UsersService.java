package kr.or.ddit.users.service;

import java.util.List;

import kr.or.ddit.vo.UsersVO;

/**
 *
 * @author 윤서현
 * @since 2025. 9. 25.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 25.     	윤서현	          최초 생성
 *  2025.10. 15.     	장어진	          Working Status 테이블 기능을 Users 테이블로 옮김.
 *
 * </pre>
 */
public interface UsersService {

	/**
	 * 조직도 구성원 추가
	 * @param user
	 * @return
	 */
	public boolean createUser(UsersVO user);
	/**
	 * 조직도 구성원 리스트 조회
	 * @return
	 */
	public List<UsersVO> readUserList();
	/**
	 * 조직도 구성원 상세조회
	 * @param userId
	 * @return
	 */
	public UsersVO readUser(String userId);
	/**
	 * 조직도 구성원 수정
	 * @param user
	 * @return
	 */
	public boolean modifyUser(UsersVO user);
	/**
	 * 조직도 구성원 퇴사자 처리
	 * @param userId
	 * @return 실제 삭제가 아니라 RSGNTN_YN = 'Y' 로 변경
	 */
	public boolean retireUser(String userId);

	/**
	 * 사용자 이름으로 사용자 목록 검색
	 * @param term 검색어 (부분 이름)
	 * @return 검색된 사용자 목록
	 */
	public List<UsersVO> searchUsers(String term);

	/**
	 * 퇴사자 목록 조회
	 * @return
	 */
	public List<UsersVO> readResignedUserList();

	/**
	 * 사용자 근무 상태 조회
	 * @param UserId
	 * @return
	 */
	public UsersVO readWorkStts(String UserId);

	/**
	 * 사용자 근무 상태 수정
	 * @param UserId
	 * @param WorkSttsCd
	 * @return
	 */
	public boolean modifyWorkStts(String UserId, String WorkSttsCd);


	/**
	 * 엑셀 업로드를 통한 사용자 일괄등록
	 * @param userList
	 * @return
	 */
	public boolean createUserList(List<UsersVO> userList);
}
