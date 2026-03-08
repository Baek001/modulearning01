package kr.or.ddit.comm.advice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import kr.or.ddit.policies.service.PoliciesService;
import kr.or.ddit.vo.PoliciesVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 사용자-권한 매핑 정보 advice Controller
 * @author 임가영
 * @since 2025. 10. 13.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 13.     	임가영           최초 생성
 *
 * </pre>
 */
@ControllerAdvice(basePackages = {
//	    "kr.or.ddit.approval",
//	    "kr.or.ddit.attendance",
//	    "kr.or.ddit.board",
//	    "kr.or.ddit.calendar",
//	    "kr.or.ddit.comm.menu.controller",
//	    "kr.or.ddit.department",
//	    "kr.or.ddit.email",
//	    "kr.or.ddit.maintask",
//	    "kr.or.ddit.menu",
//	    "kr.or.ddit.messenger",
//	    "kr.or.ddit.project",
//	    "kr.or.ddit.task"
	}, annotations = Controller.class)
@RequiredArgsConstructor
@Slf4j
public class AccessControllerAdvice {

	private final PoliciesService policiesService;

	@ModelAttribute("policiesList")
	public String accessMappingList(HttpServletRequest request) throws JsonProcessingException {

		 // 특정 URL 제외
        String requestUri = request.getRequestURI();
        log.info("=============> requestUri : {}", requestUri);
        if(requestUri.startsWith("/rest") || requestUri.startsWith("/login") || requestUri.startsWith("/error") || requestUri.startsWith("/common/auth")) {
        	return null;
        }

		List<PoliciesVO> policiesList = policiesService.readPoliciesList();

		Map<String, Object> respMap = new HashMap<>();
		for(PoliciesVO policies : policiesList) {
			String policiesFeatureId = policies.getFeatureId();
			boolean access = policiesService.checkAccess(policiesFeatureId);
			respMap.put(policiesFeatureId, access);
		}

		String policiesListJson = new ObjectMapper().writeValueAsString(respMap);
		return policiesListJson;
	}

}
