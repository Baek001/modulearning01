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
 *	2025.10. 13.		윤서현			  첨부파일 추가
 *
 * </pre>
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class NewMenuAtrzVO implements FileAttachable{

	@Include
	@NotBlank
	private Integer nwmnSqn;

	@NotBlank
	private String atrzDocId;

	private String menuNm;
	private String categoryNm;
	private LocalDate releaseYmd;
	private String standardCd;
	private Integer priceAmt;
	private Integer costRatioAmt;
	private String ingredientContent;
	private String marketingContent;

	private String atrzUserId;
    private String userNm;

    private String menuFileId;

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
 		this.menuFileId = fileId;
 	}
}
