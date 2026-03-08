package kr.or.ddit.project.mngt.service;

import java.util.List;

import kr.or.ddit.comm.paging.PaginationInfo;
import kr.or.ddit.vo.ProjectFileVO;

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
public interface ProjectFileService {

	/**
	 * 프로젝트 관련 전체 파일 조회
	 * @param bizId 프로젝트 ID
	 * @return 프로젝트 전체 파일 리스트
	 */
	public List<ProjectFileVO> getProjectAllFiles(PaginationInfo<ProjectFileVO> paging, String bizId);
}
