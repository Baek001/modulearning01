package kr.or.ddit.mybatis.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import kr.or.ddit.vo.AlarmLogVO;

/**
 * 
 * @author 임가영
 * @since 2025. 10. 8.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 8.     	임가영	        최초 생성
 *
 * </pre>
 */
@Mapper
public interface AlarmLogMapper {

	/**
	 * 알림 로그 목록 조회
	 * @param username 사용자Id
	 * @return 조회 결과 없으면 list.size() == 0.
	 */
	public List<AlarmLogVO> selectAlarmLogList(String username);
	
	/**
	 * 알림 로그 목록 10건 조회
	 * @param username 사용자Id
	 * @return 조회 결과 없으면 list.size() == 0.
	 */
	public List<AlarmLogVO> selectAlarmLogListTop10Desc(String username);
	
	/**
	 * 알림 한 건 조회
	 * @param alarmId 알림Id
	 * @return 조회 결과 없으면 null
	 */
	public AlarmLogVO selectAlarmLog(String alarmId);
	
	/**
	 * 알림 로그 데이터 삽입
	 * @param alarmLog
	 * @return 성공한 레코드 수
	 */
	public int insertAlarmLog(AlarmLogVO alarmLog);
	
	/**
	 * 알림 로그 업데이트 (읽음여부, 읽은시각 등)
	 * @param alarmLog
	 * @return 성공한 레코드 수
	 */
	public int updateAlarmLog(AlarmLogVO alarmLog);
}
