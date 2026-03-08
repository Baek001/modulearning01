package kr.or.ddit.comm.file.service;

import java.util.List;

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
 *  2025. 9. 26.     	장어진	          주석 내용 추가 및 오타 수정
 *
 * </pre>
 */
public interface FileMasterService {
	
	/**
	 * 파일 목록 조회.
	 * @return 조회 결과 없으면 list.size() == 0
	 */
	public List<FileMasterVO> readFileMasterList();
	
	/**
	 * 파일 단건 조회
	 * @param fileId : File ID
	 * @return 조회 결과 없으면 EntityNotFoundException
	 */
	public FileMasterVO readFileMaster(String fileId);
	
	/**
	 * 파일 추가
	 * @param fm : File Master VO 객체
	 * @return 성공하면 true, 실패하면 false
	 */
	public boolean createFileMaster(FileMasterVO fm);
	
	/**
	 * 파일 삭제여부 수정 (나머지 수정 불가)
	 * @param fm : File Master VO 객체
	 * @return 성공하면 true, 실패하면 false
	 */
	public boolean modifyFileMasterDelyn(FileMasterVO fm);
}
