package kr.or.ddit.vo;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import kr.or.ddit.comm.file.FileAttachable;
import kr.or.ddit.comm.validate.UpdateGroup;
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
 *  2025. 9. 24.     	장어진	        최초 생성
 *  2025. 9. 30.		임가영			파일 업로드를 위한 필드와 setter 메소드 추가
 *  2025.10.  8.		장어진			simple search를 위한 fileDetailVO, fileMaster 추가
 *  2025.10. 10.		장어진			폴더 기능 추가를 위한 컬럼 추가
 *
 * </pre>
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserFileMappingVO implements FileAttachable {

	@Include
	@NotBlank(groups = UpdateGroup.class)
	private String userFileId;

	@Include
	@NotBlank
	private String userId;

	@Include
//	@NotBlank
	private Integer userFileSqn;

	private Integer folderSqn;

	private FileDetailVO fileDetailVO;
	private FileMasterVO fileMasterVO;

	// 파일 아이콘
	private String fileIconClass;

	private String userNm;

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
		this.userFileId = fileId;
	}
}
