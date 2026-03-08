package kr.or.ddit.comm.controller;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import kr.or.ddit.dashboard.dto.CntCardDTO;
import kr.or.ddit.dashboard.dto.CurrentProjectDTO;
import kr.or.ddit.dashboard.dto.DepartStatusDTO;
import kr.or.ddit.dashboard.dto.RecentBoardDTO;
import kr.or.ddit.dashboard.dto.TodayScheduleDTO;
import kr.or.ddit.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author 임가영
 * @since 2025. 10. 15.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 15.       임가영           대시보드에 데이터 채우기 위한 service 호출
 *
 * </pre>
 */
@Controller
@RequiredArgsConstructor
public class IndexController {

	private final DashboardService service;

	@GetMapping("/")
	public String index(Model model) {

		// 출근 기록 + 근무시간 기록
		Map<String, Object> attendanceRecord = service.getAttendanceRecord();

		// 진행중인업무개수, 처리 대기 중인 결재 수, 이번주일정, 읽지않은메일개수
		CntCardDTO cntCard = service.getCntCard();

		// 오늘 일정 3개
		List<TodayScheduleDTO> todayScheduleList = service.getTodayScheDuleList();

		// 진행중인 프로젝트 3개 진행률
		List<CurrentProjectDTO> currentProjectList = service.getCurrentProjectList();

		// 최근공지사항4개
		List<RecentBoardDTO> recentBoardList = service.getRecentBoardList();

		// 자기부서에서 전체/근무중/휴가,외근 수
		DepartStatusDTO departStatus = service.getDepartStatusCnt();

		// 데이터 보내기
		model.addAttribute("attendanceRecord", attendanceRecord);
		model.addAttribute("cntCard", cntCard);
		model.addAttribute("todayScheduleList", todayScheduleList);
		model.addAttribute("currentProjectList", currentProjectList);
		model.addAttribute("recentBoardList", recentBoardList);
		model.addAttribute("departStatus", departStatus);

    	// 사이드바 메뉴 고정
		model.addAttribute("currentMenu", "dashboard");

		return "index";
	}

	@GetMapping("/websocket")
	public String getMethodName() {
		return "websocket-test";
	}

}
