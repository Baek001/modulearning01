package kr.or.ddit.department.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import kr.or.ddit.department.service.DepartmentService;
import kr.or.ddit.vo.DepartmentVO;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author 윤서현
 * @since 2025. 9. 26.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 26.     	윤서현	          최초 생성
 *
 * </pre>
 */
@RestController
@RequestMapping("/rest/comm-depart")
@RequiredArgsConstructor
public class DepartmentRestController {

	private final DepartmentService service;

	/**
	 * 조직도 부서 목록 조회. RestController
	 * @return
	 */
	@GetMapping
	public List<DepartmentVO> readDepartmentList(){
		return service.readDepartmentList();
	}

	/**
	 * 조직도 부서 상세 조회. RestController
	 * @param deptId
	 * @return
	 */
	@GetMapping("/{deptId}")
	public DepartmentVO readDepartment(@PathVariable String deptId) {
		return service.readDepartment(deptId);
	}


	/**
	 * 조직도 부서 등록
	 * @param deptVo
	 * @return
	 */
	@PostMapping
	public boolean createDepartment(@RequestBody DepartmentVO deptVo) {
		return service.createDepartment(deptVo);
	}

	/**
	 * 조직도 부서 삭제
	 * @param deptId
	 * @return USE_YN 삭제시 Y->N으로 변경
	 */
	@DeleteMapping("/{deptId}")
	public Map<String, Object> deleteDepartment(
		@PathVariable String deptId
	) {
		Map<String, Object> result = new HashMap<>();

		boolean success = service.removeDepartment(deptId);

		if (success) {
	        result.put("success", true);
	        result.put("message", "부서가 정상적으로 삭제(비활성화)되었습니다.");
	    } else {
	        result.put("success", false);
	        result.put("message", "부서에 소속된 인원이 있어 삭제할 수 없습니다.");
	    }

	    return result;
	}

	/**
	 * 조직도 부서 수정
	 * @param deptId
	 * @param vo
	 * @return
	 */
	@PutMapping("/{deptId}")
	public boolean modifyDepartment(
		@PathVariable String deptId,
		@RequestBody DepartmentVO vo
	) {
		vo.setDeptId(deptId);

		return service.modifyDepartment(vo);
	}
}
