package kr.or.ddit.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 
 * @author 임가영
 * @since 2025. 10. 7.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 7.     	임가영	          최초 생성
 *
 * </pre>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor // 역직렬화를 위해 필요(STOMP는 Jackson 기반으로 메시지를 JSON ↔ 객체 변환하기 때문에, 매개변수 없는 기본 생성자가 필요할 수 있다
public class NotificationTemplate {

	private String receiverId;
	private String senderId;
	private String alarmCode;
	private String template;
	private String alarmMessage;
	private String alarmCategory;
	private String relatedUrl;
	
}
