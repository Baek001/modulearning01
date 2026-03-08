package kr.or.ddit.mybatis.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
 *   수정일      			수정자           	  수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 25.     	윤서현	          최초 생성
 *  2025.10. 15.     	장어진	          Working Status 테이블 기능을 Users 테이블로 옮김.
 *  2025. 10. 16.		임가영			  selectUsersByDept(부서별 구성원 조회) 추가
 * </pre>
 */
@Mapper
public interface UsersMapper {

	/**
	 * 조직도 구성원 추가
	 * @param newUser
	 * @return
	 */
	public int insertUser(UsersVO user);
	/**
	 * 조직도 구성원 리스트 조회
	 * @return
	 */
	public List<UsersVO> selectUserList();
	/**
	 * 조직도 구성원 상세조회
	 * @param userId
	 * @return
	 */
	public UsersVO selectUser(String userId);
	/**
	 * 조직도 구성원 수정
	 * @param user
	 * @return
	 */
	public int updateUser(UsersVO user);
	/**
	 * 조직도 구성원 퇴사자 처리
	 * @param userId
	 * @return 실제 삭제가 아니라 RSGNTN_YN = 'Y' 로 변경
	 */
	public int retireUser(String userId);

	/**
	 * 사용자 이름으로 사용자 목록 검색
	 * @param term 검색어 (부분 이름)
	 * @return 검색된 사용자 목록
	 */
	public List<UsersVO> selectUsersByTerm(String term);

	/**
	 * 퇴사자 목록 조회
	 * @return
	 */
	public List<UsersVO> selectResignedUserList();

	/**
	 * 구성원 근무 상태 조회
	 * @param UserId
	 * @return
	 */
	public UsersVO selectWorkStts(String UserId);

	/**
	 * 구성원 근무 상태 수정
	 * @param UserId
	 * @param WorkSttsCd
	 * @return
	 */
	public int updateWorkStts(@Param("userId") String userId, @Param("workSttsCd") String workSttsCd);

	/**
	 * 기존 사용자 부서/직급 조회
	 * @param userId
	 * @return
	 */
	public UsersVO selectUserById(String userId);

	/**
	 * 부서별 구성원 조회 (**가영 추가)
	 * @param deptId 부서 Id
	 * @return
	 */
	public List<UsersVO> selectUsersByDept(String deptId);

	/**
	 * 재직증명서 발급
	 * @param userId
	 * @return
	 */
	public Map<String, Object> selectUserForCertificate(@Param("userId") String userId);

	}
