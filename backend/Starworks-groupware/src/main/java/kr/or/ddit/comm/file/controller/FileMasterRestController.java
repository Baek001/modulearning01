package kr.or.ddit.comm.file.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kr.or.ddit.comm.file.service.FileMasterService;
import kr.or.ddit.vo.FileMasterVO;
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
@RequestMapping("/rest/comm-filemaster")
@RequiredArgsConstructor
public class FileMasterRestController {
	
	private final FileMasterService service;
	
	@GetMapping
	public List<FileMasterVO> readFileMasterList(){
		return service.readFileMasterList();
	}
	
	@GetMapping("/{fileId}")
	public FileMasterVO readFileMaster(@PathVariable String fileId ) {
		return service.readFileMaster(fileId);
	}
}
