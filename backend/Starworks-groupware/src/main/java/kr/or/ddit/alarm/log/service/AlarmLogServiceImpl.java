package kr.or.ddit.alarm.log.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import kr.or.ddit.comm.exception.EntityNotFoundException;
import kr.or.ddit.mybatis.mapper.AlarmLogMapper;
import kr.or.ddit.vo.AlarmLogVO;
import lombok.RequiredArgsConstructor;

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
 *  2025. 10. 8.     	임가영	       최초 생성
 *
 * </pre>
 */
@Service
@RequiredArgsConstructor
public class AlarmLogServiceImpl implements AlarmLogService{

	private final AlarmLogMapper mapper;
	
	/**
	 * 알림 로그 목록 조회
	 */
	@Override
	public List<AlarmLogVO> readAlarmLogList(String username) {
		return mapper.selectAlarmLogList(username);
	}
	
	/**
	 * 알림 로그 목록 10건 조회
	 * @param username 사용자Id
	 * @return 조회 결과 없으면 list.size() == 0.
	 */
	public List<AlarmLogVO> readAlarmLogListTop10Desc(String username) {
		return mapper.selectAlarmLogListTop10Desc(username);
	}

	/**
	 * 알림 한 건 조회
	 */
	@Override
	public AlarmLogVO readAlarmLog(String alarmId) {
		AlarmLogVO alarmLog = mapper.selectAlarmLog(alarmId);
		if (alarmLog == null) {
			throw new EntityNotFoundException(alarmLog);
		}
		
		return alarmLog;
	}

	/**
	 * 알림 로그 데이터 삽입
	 */
	@Override
	public boolean createAlarmLog(AlarmLogVO alarmLog) {
		return mapper.insertAlarmLog(alarmLog) > 0;
	}

	/**
	 * 알림 로그 업데이트 (읽음여부, 읽은시각 등)
	 */
	@Override
	public boolean modifyAlarmLog() {
		int rowcnt = 0;
		boolean success = false;
		
		// 서비스에서 Authentication 가져오기
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		
		List<AlarmLogVO> alarmLogList = mapper.selectAlarmLogList(authentication.getName());
		
		
		for(AlarmLogVO alarmLogTemp : alarmLogList) {
			alarmLogTemp.setReadDt(LocalDateTime.now());
			alarmLogTemp.setReadYn("Y");
			rowcnt += mapper.updateAlarmLog(alarmLogTemp);
		}
		
		if (rowcnt == alarmLogList.size()) {
			success = true;
		}
		
		return success;
	}
}
