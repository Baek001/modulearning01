package kr.or.ddit.alarm.log.controller;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import kr.or.ddit.alarm.log.service.AlarmLogService;
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
 *   수정일      			수정자           	수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 8.     	임가영	        최초 생성
 *
 * </pre>
 */
@RestController
@RequiredArgsConstructor
public class AlarmLogRestController {

	private final AlarmLogService service;

	/**
	 * 알림 로그 전체 목록 조회
	 * @param receiverId 수신자Id
	 * @return 조회 결과 없으면 list.size() == 0.
	 */
	@GetMapping("/rest/alarm-log-list")
	public List<AlarmLogVO> readAlarmLogList(Authentication authentication) {
		String username = authentication.getName();
		return service.readAlarmLogList(username);
	}

	/**
	 * 알림 로그 목록 10건 조회
	 * @param receiverId 수신자Id
	 * @return 조회 결과 없으면 list.size() == 0.
	 */
	@GetMapping("/rest/alarm-log-top10")
	public List<AlarmLogVO> readAlarmLogListTop10Desc(Authentication authentication) {
		String username = authentication.getName();
		return service.readAlarmLogListTop10Desc(username);
	}

	/**
	 * 알림 한 건 조회
	 * @param alarmId 알림Id
	 * @return 조회 결과 없으면 null
	 */
	@GetMapping("/rest/alarm-log/{alarmId}")
	public AlarmLogVO readAlarmLog(@PathVariable String alarmId) {
		return service.readAlarmLog(alarmId);
	}

	/**
	 * 알림 로그 데이터 삽입
	 * @param AlarmLog
	 * @return 성공하면 true, 실패하면 false
	 */
	@PostMapping("/rest/alarm-log-list")
	public Map<String, Object> createAlarmLog(@RequestBody AlarmLogVO alarmLog) {
		boolean success = service.createAlarmLog(alarmLog);
		return Map.of("success", success);
	}

	/**
	 * 알림 로그 업데이트 (읽음여부, 읽은시각 등)
	 * @param AlarmLog
	 * @return 성공하면 true, 실패하면 false
	 */
	@PutMapping("/rest/alarm-log-list")
	public Map<String, Object> modifyAlarmLog() {
		boolean success  = service.modifyAlarmLog();
		return Map.of("success", success);
	}
}
