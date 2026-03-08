package kr.or.ddit.vo;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import kr.or.ddit.comm.file.FileAttachable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.Include;

/**
 *
 * @author 윤서현
 * @since 2025. 10. 1.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 1.     	윤서현	          최초 생성
 *
 * </pre>
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AuthorizationTempVO implements FileAttachable {

	@Include
	@NotBlank
	private String atrzTempSqn;

	@NotBlank
	private String atrzUserId;

	@Include
	@NotBlank
	private String atrzDocTmplId;
	private String atrzDocTtl;
	private LocalDateTime atrzSbmtDt;
	private String delYn;
	private String htmlData;
	private String openYn;
	private String atrzFileId;

	private List<MultipartFile> fileList;

	private String atrzDocTmplNm;

	//private List<AuthorizationLineVO> approvalLines;

	@Override
	public List<MultipartFile> getFileList() {
		return fileList;
	}

	@Override
	public void setFileId(String fileId) {
		this.atrzFileId = fileId;
	}

}
