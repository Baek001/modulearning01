package kr.or.ddit.mybatis.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import kr.or.ddit.vo.FileMasterVO;

/**
 * 
 * @author 장어진
 * @since 2025. 9. 25.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 25.     	장어진	          최초 생성
 *  2025. 9. 26.     	장어진	          주석 내용 추가
 *
 * </pre>
 */
@Mapper
public interface FileMasterMapper {
	
	/**
	 * 다음 파일 Id 조회
	 * @return 다음 파일 Id
	 */
	public String selectFileMasterFileID();
	
	/**
	 * 파일 목록 조회.
	 * @return 조회 결과 없으면 list.size() == 0
	 */
	public List<FileMasterVO> selectFileMasterList();
	
	/**
	 * 파일 단건 조회.
	 * @param fileId File ID
	 * @return 조회 결과 없을 시 null
	 */
	public FileMasterVO selectFileMaster(@Param("fileId") String fileId);
	
	/**
	 * 파일 추가.
	 * @param fm : File Master VO 객체
	 * @return 성공 시 1, 실패시 0
	 */
	public int insertFileMaster(FileMasterVO fm);
	
	/**
	 * 파일 삭제여부 수정. (나머지 수정 불가)
	 * @param fm : File Master VO 객체
	 * @return 성공 시 1, 실패시 0
	 */
	public int updateFileMasterDelyn(FileMasterVO fm);
	
	/**
	 * (자료실 파일 삭제용) 사용자, 파일 seq 로 파일이 존재하는지 조회
	 * @param fm
	 * @return
	 */
	public FileMasterVO selectFileMasterByUser(FileMasterVO fm);
}
