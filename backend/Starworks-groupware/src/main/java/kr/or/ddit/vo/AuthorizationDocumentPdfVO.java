package kr.or.ddit.vo;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.Include;

/**
 * 
 * @author 임가영
 * @since 2025. 10. 2.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 2.     	임가영           최초 생성
 *
 * </pre>
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AuthorizationDocumentPdfVO {
	
	@Include
	@NotBlank
	private String atrzDocId;
	
	private String saveFileNm;
	
	private String filePath;
	
	private String extFile;
	
	private Long fileSize;
	
	private String fileMimeType;
	
	private LocalDateTime crtDt;
	
}
