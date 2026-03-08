package kr.or.ddit.alarm.log.service;

import java.util.List;

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
public interface AlarmLogService {

	/**
	 * 알림 로그 목록 조회
	 * @param username 사용자Id
	 * @return 조회 결과 없으면 list.size() == 0.
	 */
	public List<AlarmLogVO> readAlarmLogList(String username);
	
	/**
	 * 알림 로그 목록 10건 조회
	 * @param username 사용자Id
	 * @return 조회 결과 없으면 list.size() == 0.
	 */
	public List<AlarmLogVO> readAlarmLogListTop10Desc(String username);
	
	/**
	 * 알림 한 건 조회
	 * @param alarmId 알림Id
	 * @return 조회 결과 없으면 CustomNotFoundException 발생
	 */
	public AlarmLogVO readAlarmLog(String alarmId);
	
	/**
	 * 알림 로그 데이터 삽입
	 * @param alarmLog
	 * @return 성공하면 true, 실패하면 false
	 */
	public boolean createAlarmLog(AlarmLogVO alarmLog);
	
	/**
	 * 알림 로그 전체 업데이트 (읽음여부, 읽은시각 등)
	 * @param
	 * @return 
	 */
	public boolean modifyAlarmLog();
}
