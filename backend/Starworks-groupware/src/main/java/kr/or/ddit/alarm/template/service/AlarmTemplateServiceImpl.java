package kr.or.ddit.alarm.template.service;

import java.util.List;

import org.springframework.stereotype.Service;

import kr.or.ddit.comm.exception.EntityNotFoundException;
import kr.or.ddit.mybatis.mapper.AlarmTemplateMapper;
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
 *   수정일      			수정자           	  수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 26.     	임가영	          최초 생성
 *
 * </pre>
 */
@Service
@RequiredArgsConstructor
public class AlarmTemplateServiceImpl implements AlarmTemplateService {

	private final AlarmTemplateMapper mapper;
	
	/**
	 * 알림 템플릿 전체 목록 조회.
	 */
	@Override
	public List<AlarmTemplateVO> readAlarmTemplateList() {
		return mapper.selectAlarmTemplateList();
	}

	/**
	 * 알림 템플릿 한 건 조회.
	 */
	@Override
	public AlarmTemplateVO readAlarmTemplate(String alrmId) {
		AlarmTemplateVO AlarmTemplate = mapper.selectAlarmTemplate(alrmId);
		if(AlarmTemplate == null) {
			throw new EntityNotFoundException(alrmId);
		}
		
		return AlarmTemplate;
	}
	
	
}
