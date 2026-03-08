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
 *  2025. 9. 29. 		김주민 			  rnum 추가
 *  2025. 9. 30.		김주민 			  bizPicNm, participants, viewers 추가
 *  2025. 9. 30.		임가영			  파일 업로드를 위한 필드와 setter 메소드 추가
 *  2025. 10. 13. 		김주민			  검색을 위한 searchStrtBizDt,searchEndBizDt 임시필드 추가
 *
 * </pre>
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ProjectVO implements FileAttachable {
	private int rnum;
	private String myRole;
	private List<ProjectMemberVO> members; // ProjectMemberVO 리스트

	@Include
	@NotBlank(groups = UpdateGroup.class)
	private String bizId; //프로젝트 ID

	@NotBlank(groups = InsertGroup.class)
	private String bizNm; //프로젝트 명

	/* @NotBlank(groups = InsertGroup.class) */
	private String bizPicId; //프로젝트 책임자 ID

	@NotBlank(groups = InsertGroup.class)
	private String bizTypeCd; //프로젝트 유형 코드
	private String bizTypeNm; //프로젝트 유형명

	@NotBlank(groups = UpdateGroup.class)
	private String bizSttsCd; //프로젝트 상태 코드

	@NotBlank(groups = {InsertGroup.class, UpdateGroup.class})
	private String bizGoal; //프로젝트 목표
	private String bizDetail; //프로젝트 상세설명
	private String bizScope; //프로젝트 범위
	private Integer bizBdgt; //프로젝트 예산
	private Integer bizPrgrs; //프로젝트 진행도
	private LocalDateTime strtBizDt; //프로젝트 시작일시
	private LocalDateTime endBizDt; //프로젝트 종료일시
	private String bizFileId; //프로젝트 파일ID

	private String bizPicNm;  // 책임자 이름
	private String bizPicDeptNm; // 책임자 부서명
	private String bizUserJobNm; // 직급명

	// 검색용 임시 필드 (DB 매핑 X)
    private String searchStrtBizDt;  // 검색용 시작일 (String)
    private String searchEndBizDt;   // 검색용 종료일 (String)

    private boolean hasAccess; // 접근 권한 여부
    public boolean isHasAccess() {
        return hasAccess;
    }
    public void setHasAccess(boolean hasAccess) {
        this.hasAccess = hasAccess;
    }

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
		this.bizFileId = fileId;
	}
	private String filePath;
}
