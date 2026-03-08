package kr.or.ddit.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.Include;

/**
 * 
 * @author 장어진
 * @since 2025. 9. 24.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 24.     	장어진	        최초 생성
 *	2025. 9. 27.		임가영			fileSize 타입 Integer -> Long 변경
 *	2025.10. 10.		장어진			delYn 컬럼 추가
 * </pre>
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class FileDetailVO {
	
	@Include
	@NotBlank
	private String fileId;
	
	@Include
	@NotBlank
	private Integer fileSeq;
	
	private String orgnFileNm;
	
	@NotBlank
	private String saveFileNm;
	
	@NotBlank
	private String extFile;
	
	@NotBlank
	private String fileMimeType;
	
	@NotBlank
	private String filePath;
	
	@NotBlank
	private Long fileSize;
	
	private String delYn;
}
