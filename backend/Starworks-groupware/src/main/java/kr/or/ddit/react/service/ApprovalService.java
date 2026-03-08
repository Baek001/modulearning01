package kr.or.ddit.react.service;

import java.util.List;
import java.util.Map;

/**
*
* @author 홍현택
* @since 2025. 10. 17.
* @see
*
* <pre>
* << 개정이력(Modification Information) >>
*
*   수정일      			수정자           수정내용
*  -----------   	-------------    ---------------------------
*  2025. 10. 17.     	홍현택	          최초 생성
*
* </pre>
*/
public interface ApprovalService {
    /**
     * 월별 결재 양식 사용량 및 카테고리별 통계를 조회합니다.
     * @return 월, 카테고리, 개수를 포함하는 맵 리스트
     */
    List<Map<String, Object>> getMonthlyApprovalUsageByCategory();
}
