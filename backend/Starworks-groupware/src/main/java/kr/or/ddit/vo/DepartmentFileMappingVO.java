package kr.or.ddit.vo;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.NotBlank;
import kr.or.ddit.comm.file.FileAttachable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.Include;
import lombok.ToString;

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
 *  2025. 9. 24.     	장어진	          최초 생성
 *  2025. 10. 9.		임가영			fileMaster, fileDetail, userNm, rnum 담을 수 있는 필드 추가
 * </pre>
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DepartmentFileMappingVO implements FileAttachable {

	@Include
	@NotBlank
	private String deptId;

	@Include
	@NotBlank
	private String deptFileId;

	// usersVO
	private String userNm;

	// 직급, 부서
	private String jbgdNm;
	private String deptNm;

	// fileMasterVO
	private String crtUserId;
	private LocalDateTime crtDt;

	// fileDetailVO
	private String orgnFileNm;
	private String saveFileNm;
	private String filePath;
	private Long fileSize;
	private String fileMimeType;
	private String fileSeq;

	// 파일 아이콘
	private String fileIconClass;

	// 파일 업로드를 위한..
	@JsonIgnore
	@ToString.Exclude
	private List<MultipartFile> fileList;

	@Override
	public List<MultipartFile> getFileList() {
		return this.fileList;
	}
	@Override
	public void setFileId(String fileId) {
		this.deptFileId = fileId;
	}
}
