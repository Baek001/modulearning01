package kr.or.ddit.alarm.log.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import kr.or.ddit.alarm.log.service.AlarmLogService;
import kr.or.ddit.vo.AlarmLogVO;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author 임가영
 * @since 2025. 10. 24.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 24.     	임가영           최초 생성
 *
 * </pre>
 */
@Controller
@RequiredArgsConstructor
public class AlarmLogReadController {

	private final AlarmLogService service;

	@GetMapping("/alarm/all")
	public String readAlarmLog(
		Authentication authentication
		, Model model
	) {
		List<AlarmLogVO> alarmLogList = service.readAlarmLogList(authentication.getName());
		model.addAttribute("alarmLogList", alarmLogList);
		return "alarm/alarm-all-main";
	}
}
