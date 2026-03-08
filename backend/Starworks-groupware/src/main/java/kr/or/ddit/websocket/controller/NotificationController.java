package kr.or.ddit.websocket.controller;

import java.util.Map;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import kr.or.ddit.websocket.service.impl.NotificationServiceImpl;
import lombok.RequiredArgsConstructor;

/**
 * 알림 전송 컨트롤러
 * @author 임가영
 * @since 2025. 10. 7.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 7.     	임가영	       최초 생성
 *
 * </pre>
 */
@Controller
@RequiredArgsConstructor
public class NotificationController {
	
	private final NotificationServiceImpl notificationService;
	
	@MessageMapping("/notify") // <= 클라이언트가 /app/notify 로 메시지 보낼 때 처리
//	@SendTo("/topic/notify") <= 정적 경로만 가능. 특정 사용자에게만 알림을 보내고 싶다면 SimpMessagingTemplate 사용
	public void handleNotification(Map<String, Object> payload) {
		notificationService.sendNotification(payload);
	
	}
}
	