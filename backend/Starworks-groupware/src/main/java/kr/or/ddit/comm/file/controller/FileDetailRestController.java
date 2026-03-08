package kr.or.ddit.comm.file.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kr.or.ddit.comm.file.service.FileDetailService;
import kr.or.ddit.vo.FileDetailVO;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author 장어진
 * @since 2025. 9. 26.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 26.     	장어진	          최초 생성
 *
 * </pre>
 */
@RestController
@RequiredArgsConstructor
public class FileDetailRestController {

	private final FileDetailService service;

	@GetMapping("/rest/comm-file")
	public List<FileDetailVO> readFileDetailList(String fileId){
		return service.readFileDetailList(fileId);
	}

	/**
	 * 파일 상세 단건 조회
	 * @param fileId
	 * @param fileSeq
	 * @return
	 */
	@GetMapping("/rest/comm-file/{fileSeq}")
	public FileDetailVO readFileDetail(
		@PathVariable(name = "fileSeq") Integer fileSeq) {
		return service.readFileDetail(fileSeq);
	}
}
