package kr.or.ddit.project.mngt.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import kr.or.ddit.comm.paging.PaginationInfo;
import kr.or.ddit.comm.paging.renderer.MazerPaginationRenderer;
import kr.or.ddit.comm.paging.renderer.PaginationRenderer;
import kr.or.ddit.project.mngt.service.ProjectFileService;
import kr.or.ddit.vo.ProjectFileVO;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author 김주민
 * @since 2025. 10. 13.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 13.     	김주민	          최초 생성
 *
 * </pre>
 */
@RestController
@RequestMapping("/rest/project-files")
@RequiredArgsConstructor
public class ProjectFilesRestController {

	private final ProjectFileService projectFileService;

	/**
	 * 프로젝트 관련 파일 전체 조회
	 *
	 * - 프로젝트 등록 첨부파일
	 * - 업무 첨부파일
	 * - 게시판 첨부파일
	 *
	 * @param bizId 프로젝트 ID
	 * @return
	 */
	@GetMapping("/{bizId}/files")
    public Map<String, Object> getProjectAllFiles(
    	@PathVariable String bizId,
    	@RequestParam(required = false, defaultValue = "1") int page
    ) {
        Map<String, Object> result = new HashMap<>();

        try {
        	 // 페이징 정보 설정
            PaginationInfo<ProjectFileVO> paging = new PaginationInfo<>();
            paging.setCurrentPage(page);

            // 프로젝트 관련 전체 파일 조회
            List<ProjectFileVO> fileList = projectFileService.getProjectAllFiles(paging,bizId);

            // 페이징 HTML 생성
            PaginationRenderer renderer = new MazerPaginationRenderer();
            String pagingHTML = renderer.renderPagination(paging, "fnFilesPaging");

            result.put("success", true);
            result.put("fileList", fileList);
            result.put("totalCount", fileList.size()); //파일 총개수 카운트
            result.put("totalRecord", paging.getTotalRecord()); //페이징처리
            result.put("pagingHTML", pagingHTML);

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "파일 목록 조회에 실패했습니다: " + e.getMessage());
            result.put("fileList", new ArrayList<>());
        }

        return result;
    }
}
