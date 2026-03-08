package kr.or.ddit.mybatis.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import kr.or.ddit.comm.paging.PaginationInfo;
import kr.or.ddit.vo.ProjectFileVO;
import kr.or.ddit.vo.ProjectVO;

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
 *  2025. 10. 22.		김주민			  파일 모아보기 페이징처리를 위해 selectTotalFileCount 추가
 *
 * </pre>
 */
@Mapper
public interface ProjectFileMapper {

	int selectTotalFileCount(String bizId);
	/**
	 * 프로젝트 관련 전체 파일 조회
	 * @param bizId 프로젝트 ID
	 * @return
	 */
	public List<ProjectFileVO> selectProjectAllFiles(PaginationInfo<ProjectFileVO> paging, String bizId);
}
