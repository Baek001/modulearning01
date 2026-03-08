package kr.or.ddit.alarm.template.service;

import java.util.List;

import kr.or.ddit.vo.AlarmTemplateVO;

/**
 * 
 * @author 임가영
 * @since 2025. 9. 26.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           		수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 26.     	임가영	            최초 생성
 *
 * </pre>
 */
public interface AlarmTemplateService {
	
	/**
	 * 알림 템플릿 전체 목록 조회.
	 * @return 조회 결과 없으면 list.size() == 0
	 */
	public List<AlarmTemplateVO> readAlarmTemplateList();
	
	/**
	 * 알림 템플릿 한 건 조회.
	 * @param alrmId 알림 템플릿 Id
	 * @return 조회 결과 없으면 EntityNotFoundException
	 */
	public AlarmTemplateVO readAlarmTemplate(String alrmId);
}
