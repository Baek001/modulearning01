package kr.or.ddit.mybatis.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

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
 *  2025. 9. 26.     	임가영	       		최초 생성
 *
 * </pre>
 */
@Mapper
public interface AlarmTemplateMapper {
	
	/**
	 * 알림 템플릿 전체 목록 조회.
	 * @return 조회 결과 없으면 list.size() == 0
	 */
	public List<AlarmTemplateVO> selectAlarmTemplateList();
	
	/**
	 * 알림 템플릿 한 건 조회.
	 * @param alrmId 알림 템플릿 Id
	 * @return 조회 결과 없으면 null
	 */
	public AlarmTemplateVO selectAlarmTemplate(String alrmId);
}
