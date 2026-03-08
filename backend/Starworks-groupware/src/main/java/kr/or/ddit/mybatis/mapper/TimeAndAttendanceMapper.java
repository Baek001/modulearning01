package kr.or.ddit.mybatis.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import kr.or.ddit.vo.TimeAndAttendanceVO;

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
 *  2025. 9. 25.     	장어진	          주석 내용 추가
 *
 * </pre>
 */
@Mapper
public interface TimeAndAttendanceMapper {

	/**
	 * 근무 현황 목록 조회
	 * @return 조회 결과 없으면 list.size() == 0
	 */
	public List<TimeAndAttendanceVO> selectTimeAndAttendanceList();

	/**
	 * 사용자 근무 현황 조회
	 * @param userId : 사용자 ID
	 * @return 조회 결과 없으면 list.size() == 0
	 */
	public List<TimeAndAttendanceVO> selectUserTimeAndAttendance(String userId);

	/**
	 * 근무 현황 단건 조회
	 * @param userId : 사용자 ID
	 * @param workYmd : 근무 일자
	 * @return 조회 결과 없을 시 null
	 */
	public TimeAndAttendanceVO selectTimeAndAttendance(@Param("userId") String userId, @Param("workYmd") String workYmd);

	/**
	 * 근무 현황 추가
	 * @param taa : TimeAndAttendance VO 객체
	 * @return 성공 시 1, 실패 시 0
	 */
	public int insertTimeAndAttendance(TimeAndAttendanceVO taa);

	/**
	 * 근무 현황 추가 (스케줄러 용)
	 * @param taa : TimeAndAttendance VO 객체
	 * @return 성공 시 1, 실패 시 0
	 */
	public int insertTimeAndAttendanceForSchedule(TimeAndAttendanceVO taa);

	/**
	 * 근무 현황 수정
	 * @param taa : TimeAndAttendance VO 객체
	 * @return 성공 시 1, 실패 시 0
	 */
	public int updateTimeAndAttendance(TimeAndAttendanceVO taa);

	/**
	 * 근무 현황 수정 (스케줄러 용)
	 * @param taa : TimeAndAttendance VO 객체
	 * @return 성공 시 1, 실패 시 0
	 */
	public int updateTimeAndAttendanceForSchedule(TimeAndAttendanceVO taa);




}
