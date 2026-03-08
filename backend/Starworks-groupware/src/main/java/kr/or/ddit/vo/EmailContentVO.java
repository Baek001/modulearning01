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
 *  2025. 9. 27.     	장어진	          LocalDate -> LocalDateTime으로 수정
 *	2025. 9. 30.		임가영			파일 업로드를 위한 필드와 setter 메소드 추가
 *	2025. 10.13.		홍현택			발신,수신자, 읽음 여부 추가를 위한 프로퍼티 추가
 *	2025. 10.13. 		홍현택  			파일 업로드 부분 수정 및 추가
 *  2025. 10.14.		홍현택			첨부파일과 중요표시를 위한 프로퍼티 추가
 * </pre>
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class EmailContentVO implements FileAttachable {

	// jsp에서 넘어오는 받는 사람 목록을 담기위한 필드
	private String[] recipients;


	@Include
	@NotBlank
	private String emailContId;

	@NotBlank
	private String userId;
	private String subject;
	private String content;
	private LocalDateTime sendDate;

	// JSTL <fmt:formatDate> 태그 호환을 위해 java.util.Date 반환 메서드 추가
	public java.util.Date getSendDateAsUtilDate() {
	    if (this.sendDate == null) {
	        return null;
	    }
	    return java.sql.Timestamp.valueOf(this.sendDate);
	}

	private String mailFileId; //첨부파일
	private String imptMailYn; // 중요 표시
	private String isUserImportant; // 현재 사용자가 중요 메일로 표시했는지 여부 DB에 없는 컬럼..
	private String senderName; // 발신자 이름 추가
	private String senderDeptName; // 발신자 부서 이름 추가
	private String senderEmail; // 발신자 이메일 추가
	private String readYn;     // 읽음 여부 추가
	private List<EmailReceiverVO> receiverList; // 수신자 목록 추가
	private List<UsersVO> recipientUserList; // 화면 표시용 수신자 상세 정보 목록
	private List<FileDetailVO> attachmentList; // 첨부파일 목록

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
		this.mailFileId = fileId;
	}
}
