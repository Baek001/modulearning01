package kr.or.ddit.vacation.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kr.or.ddit.security.CustomUserDetails;
import kr.or.ddit.vacation.service.VactionService;
import kr.or.ddit.vo.VactionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
 *  2025. 10. 23.     	장어진            연차 계산 기능 추가
 *
 * </pre>
 */
@Slf4j
@RestController
@RequestMapping("/rest/approval-vacation")
@RequiredArgsConstructor
public class VacationRestController {

	private final VactionService service;

	/**
	 * 전자결재 휴가원일정 캘린더 목록 조회
	 * @return
	 */
	@GetMapping
	public List<VactionVO> readVactionList(){
		return service.readVactionList();
	}

	/**
	 * 전자결재 휴가원일정 캘린더 상세조회
	 * @param vactSqn
	 * @return
	 */
	@GetMapping("/{vactSqn}")
	public VactionVO readVaction(@PathVariable String vactSqn) {
		return service.readVaction(vactSqn);
	}

	/**
	 * 전자결재 휴가일정 사용자 연차 계산용 조회
	 * @param authentication
	 * @return
	 */
	@GetMapping("/E101")
	public int readVacation(Authentication authentication) {
		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
		log.info("userDetails : {}", userDetails);
		Integer year = LocalDate.now().getYear();

		return service.readVacationDaysByUser(userDetails.getRealUser().getUserId(), "E101", year);
	}
}
