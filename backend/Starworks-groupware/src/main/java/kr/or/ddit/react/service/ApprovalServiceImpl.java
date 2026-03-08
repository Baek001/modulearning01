package kr.or.ddit.react.service;

import kr.or.ddit.mybatis.mapper.AuthorizationDocumentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
@Service
public class ApprovalServiceImpl implements ApprovalService {

    @Autowired
    private AuthorizationDocumentMapper authorizationDocumentMapper;

    @Override
    public List<Map<String, Object>> getMonthlyApprovalUsageByCategory() {
        List<Map<String, Object>> rawData = authorizationDocumentMapper.selectMonthlyApprovalUsageByCategory();


        return rawData.stream().map(item -> {
            Map<String, Object> lowercasedMap = new HashMap<>();
            item.forEach((key, value) -> lowercasedMap.put(key.toLowerCase(), value));
            return lowercasedMap;
        }).collect(Collectors.toList());
    }
}
