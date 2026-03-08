package kr.or.ddit.project.mngt.service;

import java.util.List;

import org.springframework.stereotype.Service;

import kr.or.ddit.comm.paging.PaginationInfo;
import kr.or.ddit.mybatis.mapper.ProjectFileMapper;
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
@Service
@RequiredArgsConstructor
public class ProjectFileServiceImpl implements ProjectFileService{

	private final ProjectFileMapper projectFileMapper;

	@Override
	public List<ProjectFileVO> getProjectAllFiles(PaginationInfo<ProjectFileVO> paging, String bizId) {
		// 전체 레코드 수 조회
        int totalRecord = projectFileMapper.selectTotalFileCount(bizId);
        paging.setTotalRecord(totalRecord);

        // 파일 목록 조회
        return projectFileMapper.selectProjectAllFiles(paging, bizId);
	}


}
