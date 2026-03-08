package kr.or.ddit.vo;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.NotBlank;
import kr.or.ddit.comm.file.FileAttachable;
import kr.or.ddit.comm.validate.InsertGroup;
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
 *  2025. 9. 24.     	장어진	          최초 생성
 *  2025. 9. 27.     	장어진	          LocalDate -> LocalDateTime으로 수정
 *  2025.10. 02.     	장어진	          기능 제작을 위한 요소 추가
 *
 * </pre>
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ProjectBoardVO implements FileAttachable {
	
	@Include
	@NotBlank(groups = UpdateGroup.class)
	private String bizPstId;
	
	@NotBlank(groups = InsertGroup.class)
	private String bizId;
	
	@NotBlank
	private String pstTtl;
	private String contents;
	private Integer viewCnt;
	private String delYn;
	
	@NotBlank(groups = InsertGroup.class)
	private String crtUserId;
	private LocalDateTime frstCrtDt;
	private String lastChgUserId;
	private LocalDateTime lastChgDt;
	private String bizPstFileId;
	
	private String noticeYn;
	
	private UsersVO users;
//	private ProjectVO project;
	
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
		this.bizPstFileId = fileId;
	}
}
