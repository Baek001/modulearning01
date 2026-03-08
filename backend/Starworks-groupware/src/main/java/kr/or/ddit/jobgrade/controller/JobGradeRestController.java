package kr.or.ddit.jobgrade.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kr.or.ddit.jobgrade.service.JobGradeService;
import kr.or.ddit.vo.JobGradeVO;
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
@RequestMapping("/rest/comm-job")
@RequiredArgsConstructor
public class JobGradeRestController {

	private final JobGradeService service;
	
	/**
	 * 조직도 직급 목록 조회. RestController
	 * @return
	 */
	@GetMapping
	public List<JobGradeVO> readJobGradeList(){
		return service.readJobGradeList();
	}
	
	/**
	 * 조직도 직급 상세조회. RestController
	 * @param jbgdCd
	 * @return
	 */
	@GetMapping("/{jbgdCd}")
	public JobGradeVO readJobGrade(@PathVariable String jbgdCd) {
		return service.readJobGrade(jbgdCd);
	}
	
}
