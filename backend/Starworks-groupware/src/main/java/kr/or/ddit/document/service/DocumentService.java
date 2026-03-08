package kr.or.ddit.document.service;

import java.util.List;
import java.util.Map;

/**
 * 
 * @author 임가영
 * @since 2025. 10. 19.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 19.     	임가영	          최초 생성
 *
 * </pre>
 */
public interface DocumentService {
	
	/**
	 * 자료실 파일을 복구하는 서비스
	 * @param fileIds 복구할 파일id 리스트
	 * @param fileSeqs 복구할 파일sqn 리스트
	 * @return 성공하면 true, 실패하면 false
	 */
	public boolean restoreDocumentFile(List<String> fileIds, List<Integer> fileSeqs);

	/**
	 * 자료실 폴더를 복구하는 서비스
	 * @param fileIds 복구할 폴더sqn 리스트
	 * @return 성공하면 true, 실패하면 false
	 */
	public boolean restoreDocumentFolder(List<Integer> folderSqns);
	
	/**
	 * 현재 폴더 및 하위 폴더의 모든 파일 정보를 재귀적으로 조회하는 서비스 (zip 파일 변환을 위한)
	 * @param folderSqn 현재 폴더 sqn
	 * @return S3 key, orgnFileNm, folderPath 가 들어있는 Map List
	 */
	public List<Map<String, Object>> readAllFilesInFolderRecursive(Integer folderSqn);

}
