package kr.or.ddit.mybatis.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import kr.or.ddit.vo.UserScheduleVO;

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
 *
 * </pre>
 */
@Mapper
public interface UserScheduleMapper {
	/**
	 * 개인 일정 목록 조회 (페이징 X)
	 * @return 조회 결과 없으면 list.size() == 0
	 */
	public List<UserScheduleVO> selectUserScheduleList();

	public List<UserScheduleVO> selectUserScheduleListByUserId(@Param("userId") String userId);
	
	/**
	 * 개인 일정 단건 조회
	 * @param userSchdId : 사용자 일정 ID
	 * @return 조회 결과 없을 시 null
	 */
	public UserScheduleVO selectUserSchedule(@Param("userSchdId") String userSchdId);
	
	/**
	 * 개인 일정 생성
	 * @param us : User Schedule VO 객체
	 * @return 성공 시 1, 실패 시 0
	 */
	public int insertUserSchedule(UserScheduleVO us);
	
	/**
	 * 개인 일정 수정
	 * @param us : User Schedule VO 객체 
	 * @return 성공 시 1, 실패 시 0
	 */
	public int updateUserSchedule(UserScheduleVO us);

	public int softDeleteUserSchedule(@Param("userSchdId") String userSchdId);
}
