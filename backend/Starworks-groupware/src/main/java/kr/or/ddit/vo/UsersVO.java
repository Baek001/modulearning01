package kr.or.ddit.vo;

import java.time.LocalDate;
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
 *	2025. 9. 30.		임가영			파일 업로드를 위한 필드와 setter 메소드 추가
 *  2025. 10. 15.		임가영			프로필 사진 링크를 가져오기 위한 필드 추가
 *  2025. 10. 21.		홍현택			 OTP 서비스를 위한 필드 추가
 * </pre>
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UsersVO implements FileAttachable{

	@Include
	@NotBlank
	private String userId; //사용자ID

	@NotBlank
	private String userPswd; //사용자 비밀번호

	@NotBlank
	private String userNm;	//사용자 이름

	@NotBlank
	private String userEmail; //사용자 이메일

	private String userTelno; //사용자 전화번호
	private String extTel; //사용자 내선번호
	private String rsgntnYn; //퇴사여부
	private LocalDate rsgntnYmd; //퇴사일자

	@NotBlank
	private String deptId; //부서ID

	@NotBlank
	private String jbgdCd; //직책ID

	private String userImgFileId; //사용자 프로필사진 파일ID
	private String userRole; //임시 사용자 권한
	private LocalDate hireYmd; //입사일


	private String deptNm; //부서명
	private String jbgdNm; //직급명

	private String workSttsCd; //근무 상태코드
	private String codeNm; // 근무상태 이름

	private String upDeptId; //상위부서

	// 프로필 사진 정보 가져오기 위한 필드
	private String filePath; // 사용자 프로필 사진 경로

	// OTP 서비스를 위한 필드 (10.21 현택 추가)
	private String userOtpSecret;

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
		this.userImgFileId = fileId;
	}

}
