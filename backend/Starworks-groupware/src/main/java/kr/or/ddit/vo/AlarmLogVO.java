package kr.or.ddit.vo;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.Include;

/**
 * 
 * @author 임가영
 * @since 2025. 10. 8.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           		수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 8.     	임가영	       		최초 생성
 *
 * </pre>
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AlarmLogVO {
	
	@Include
	@NotBlank
	private Integer alarmId        		 ;
	
	@NotBlank
	private String receiverId   		 ;
	private String senderId     		 ;
	private String alarmCode    		 ;
	private String alarmMessage 		 ;
	private String alarmCategory		 ;
	private String readYn       		 ;
	private LocalDateTime createdDt    	 ;
	private LocalDateTime readDt         ;
	private String relatedUrl			 ;
}
