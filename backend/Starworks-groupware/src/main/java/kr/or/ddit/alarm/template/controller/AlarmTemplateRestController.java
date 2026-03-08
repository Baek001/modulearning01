package kr.or.ddit.alarm.template.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kr.or.ddit.alarm.template.service.AlarmTemplateService;
import kr.or.ddit.vo.AlarmTemplateVO;
import lombok.RequiredArgsConstructor;

/**
 * 
 * @author 임가영
 * @since 2025. 9. 26.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 26.     	임가영	       최초 생성
 *
 * </pre>
 */
@RestController
@RequestMapping("/rest/comm-alrm")
@RequiredArgsConstructor
public class AlarmTemplateRestController {
	
	
	/**
	 * conflict 테스트 점검
	 */
	private final AlarmTemplateService service;
	
	/**
	 * 알림 리스트 전체 목록 조회.
	 * @return 알림 리스트
	 */
	@GetMapping
	public List<AlarmTemplateVO> readAlarmTemplateList() {
		return service.readAlarmTemplateList();
	}
	
	/// conflict 테스트 점검 
	/// 충돌 테스트 
	/**
	 * 알림 리스트 단건 조회.
	 * @param alrmId 알림 Id
	 * @return 알림 Id 에 해당하는 알림 템플릿 등
	 */
	@GetMapping("/{alrmId}")
	public AlarmTemplateVO readAlarmTemplate(@PathVariable String alrmId) {
		return service.readAlarmTemplate(alrmId);
	}
}
