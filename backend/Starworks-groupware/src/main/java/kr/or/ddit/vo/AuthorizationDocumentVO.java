package kr.or.ddit.vo;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
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
 *  2025. 9. 24.     	장어진	        최초 생성
 *  2025. 9. 27.     	장어진	        LocalDate -> LocalDateTime으로 수정
 *  2025. 9. 30 		홍현택			전자결재 목록 조회용 프로퍼티 추가
 *	2025. 9. 30.		임가영			파일 업로드를 위한 필드와 setter 메소드 추가
 *	2025.10. 1.			임가영			Users 필드 추가
 *	2025.10.02			윤서현			전자결재 첨부파일
 * </pre>
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AuthorizationDocumentVO implements FileAttachable {

	@Include
	@NotBlank
	private String atrzDocId;

	@NotBlank
	private String atrzUserId;

	@NotBlank
	private String atrzDocTmplId;
	private String atrzDocTtl;
	private LocalDateTime atrzSbmtDt;
	private String crntAtrzStepCd;
	private String delYn;
	private String htmlData;
	private String openYn;
	private String atrzFileId;

	// =============테이블 조인으로 전자결재 목록 조회용=================
	// 템플릿명 (AUTHORIZATION_DOCUMENT_TEMPLATE)
    private String atrzDocTmplNm;
    // 템플릿 카테고리
    private String atrzCategory;


    private String drafterName;  // 기안자명
    private String drafterFilePath; // 기안자 프로필
    private String drafterJbgdNm; // 기안자 직급명
    private String drafterDeptNm; // 기안자 부서명

    // 상태명
    private String crntAtrzStepNm;

    private String myActCd;        // 내가 처리해야 할 현재 라인의 행동 코드 (AUTHORIZATION_LINE.ATRZ_ACT)
	private String myActNm;        // 행동 코드명 (COMMON_CODE.CODE_NM)
	private String myApprStts;     // 현재 라인의 상태코드 (AUTHORIZATION_LINE.ATRZ_APPR_STTS)
	    private String myApprSttsNm;   // 현재 라인의 상태명 (COMMON_CODE.CODE_NM)
		private LocalDateTime myPrcsDt;// 현재 라인의 처리일시 (AUTHORIZATION_LINE.PRCS_DT)

		// 보안등급 추가(10.25 현택 추가)
		private Integer atrzSecureLvl; //보안등급

	    // 기안자 정보를 담을 컬럼 (가영추가)
	    private UsersVO users;
    // =============상세 조회용 (결재선)=================
 	private List<AuthorizationLineVO> approvalLines;

 	// =============JSP 아이콘용 (가영추가) =================
 	private String iconClass; // bi-sun, bi-wallet2 ...
 	private String bgColorClass; // bg-warning, bg-primary ...

	// 접근 권한 여부 (부서문서함에서 사용)
	private boolean accessAllowed;

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
		this.atrzFileId = fileId;
	}

	public Date getAtrzSbmtDtAsUtilDate() {
	    if (atrzSbmtDt == null) {
	        return null;
	    }
	    return Date.from(atrzSbmtDt.atZone(ZoneId.systemDefault()).toInstant());
	}

}
