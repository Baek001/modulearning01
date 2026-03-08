package kr.or.ddit.policies.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import kr.or.ddit.comm.validate.InsertGroupNotDefault;
import kr.or.ddit.policies.dto.PolicyDTO;
import kr.or.ddit.policies.service.PoliciesService;
import kr.or.ddit.vo.PoliciesVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
@RequestMapping("/rest/comm-policies")
@RequiredArgsConstructor
@Slf4j
public class PoliciesRestController {

	private final PoliciesService service;

	/**
	 * 권한 정책 목록 조회. RestController
	 * @return 조회 결과 없으면 list.size() == 0.
	 */
	@GetMapping
	public List<PoliciesVO> readPoliciesList() {
		return service.readPoliciesList();
	}

	/**
	 * 권한 정책 단건 조회.
	 * @param featureId
	 * @return 권한정책Id, 기능Id, 설명을 담은 vo. 조회 결과 없으면 EntityNotFoundException
	 */
	@GetMapping("/{featureId}")
	public PoliciesVO readPolicies(@PathVariable String featureId) {
		return service.readPolicies(featureId);
	}

	/**
	 * 권한 정책 등록.
	 * @param paramMap featureId, remark, deptList, jbgdCd 를 담은 Map
	 * @return 성공하면 true, 실패하면 false
	 */
	@PostMapping
	public Map<String, Object> createPolicies(
			@RequestBody @Validated(InsertGroupNotDefault.class) PolicyDTO policyDTO,
			BindingResult errors) {

		Map<String, Object> respMap = new HashMap<>();
		boolean success = false;

		if (!errors.hasErrors()) {
			success = service.createPolicies(policyDTO);
		} else {
			respMap.put("message", "필수 값을 입력하세요.");
		}

		respMap.put("success", success);
		return respMap;
	}

	@PutMapping
	public Map<String, Object> modifyPolicies(
			@RequestBody @Validated(InsertGroupNotDefault.class) PolicyDTO policyDTO,
			BindingResult errors) {

		Map<String, Object> respMap = new HashMap<>();
		int rowcnt = 0;

		if (!errors.hasErrors()) {
			rowcnt = service.modifyPolicies(policyDTO);
		} else {
			respMap.put("message", "필수 값을 입력하세요.");
		}

		respMap.put("rowcnt", rowcnt);
		return respMap;
	}

	/**
	 * 권한 정책 삭제.
	 * @param featureId 삭제할 기능 Id
	 * @return
	 */
	@DeleteMapping("/{featureId}")
	public Map<String, Object> removePolicies(@PathVariable String featureId) {
		boolean success = service.removePolicies(featureId);

		return Map.of("success", success);
	}
}
