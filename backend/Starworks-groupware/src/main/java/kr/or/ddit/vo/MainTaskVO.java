package kr.or.ddit.vo;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
 *	2025. 9. 30.		임가영			파일 업로드를 위한 필드와 setter 메소드 추가
 *	2025. 10. 06.		김주민			페이징 처리를 위한 RNUM 추가, bizUserNm 추가
 * </pre>
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class MainTaskVO implements FileAttachable{

	@Include
	@NotBlank(groups = UpdateGroup.class)
	private String taskId; //업무 ID

	@NotBlank(groups = InsertGroup.class)
	private String bizId; // 프로젝트 ID

	private String bizNm; //프로젝트 명
	private String latestComment; //최근 코멘트

	@NotBlank(groups = InsertGroup.class)
	private String bizUserId; //프로젝트 인원 ID
	private String bizUserNm; //프로젝트 인원 이름(담당자명)
	private String bizUserDeptNm; // 담당자 부서명

	@NotBlank(groups = InsertGroup.class)
	private String taskNm; //업무명
	private String taskDetail; //업무 상세설명
	private Integer taskPrgrs; //업무 진행동

	@NotBlank
	private String taskSttsCd; //업무 상태 코드

	@NotNull(groups = InsertGroup.class)
	private LocalDateTime strtTaskDt; //업무 시작일시
	@NotNull(groups = InsertGroup.class)
	private LocalDateTime endTaskDt; //업무 종료일시
	private String taskFileId;

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
		this.taskFileId = fileId;
	}

	private int rnum; //페이징처리를 위한 rnum

}
