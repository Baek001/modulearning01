package kr.or.ddit.project.member.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import kr.or.ddit.project.member.service.projectMemberService;
import kr.or.ddit.vo.ProjectMemberVO;
import lombok.RequiredArgsConstructor;

//@RestController
//@RequestMapping("/플젝멤버")
@RequiredArgsConstructor
public class ProjectMemberController {
	
	private final projectMemberService service;
	
	/**
	 * 각 프로젝트 별 프로젝트 참여 인원 목록 조회 RestController
	 * @param bizId 프로젝트 일련번호
	 * @return 조회 결과 없으면 list.size() == 0
	 */
	@GetMapping("/{아무거나}")
	public  List<ProjectMemberVO> readProjectMemberList(@PathVariable String bizId) {
		return service.readProjectMemberList(bizId);
	}
	
}
