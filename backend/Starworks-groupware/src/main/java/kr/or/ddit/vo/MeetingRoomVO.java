package kr.or.ddit.vo;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.NotBlank;
import kr.or.ddit.comm.validate.UpdateGroup;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 *
 * @author 윤서현
 * @since 2025. 10. 17.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 17.     	윤서현	          최초 생성
 *
 * </pre>
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class MeetingRoomVO {

	@NotBlank(groups = UpdateGroup.class)
	private String roomId;

	@NotBlank
	private String roomName;

	private String location; // 회의실 위치

	private Integer capacity; // 수용인원

	private String useYn;

	private String delYn; // 회의실 닫힘 여부

	// rownum 필드
	private Integer rnum;


}
