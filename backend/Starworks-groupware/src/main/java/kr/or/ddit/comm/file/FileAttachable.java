package kr.or.ddit.comm.file;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

/**
 * 파일 첨부가 필요한 VO 에 implement 한다
 * @author 임가영
 * @since 2025. 9. 30.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 30.     	임가영           최초 생성
 *
 * </pre>
 */
public interface FileAttachable {
	
	/**
	 * 업로드파일로부터 멀티파트 리스트를 얻어오는 getter
	 * @return
	 */
	List<MultipartFile> getFileList();
	
	/**
	 * VO에 파일 Id 를 세팅하기위한 setter
	 * @param fileId
	 */
	void setFileId(String fileId);

}
