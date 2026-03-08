package kr.or.ddit.comm.conf;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

/**
 * 웹소켓 설정 파일
 *
 * @author 임가영
 * @since 2025. 10. 7.
 * @see
 *
 *      <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 7.     	임가영	        최초 생성
 *  2025. 10. 14. 		김주민			채팅 기능 추가
 *
 *      </pre>
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	@Override
	public void configureMessageBroker(MessageBrokerRegistry config) {
		// 구독용 경로 (클라이언트가 SUBSCRIBE 할 때)
		// /topic - 1:N
		// /queue - 1:1 - (주민)추가
		config.enableSimpleBroker("/topic", "/queue");

		// 클라이언트가 SEND 할 때 prefix
		config.setApplicationDestinationPrefixes("/app");

		// 특정 사용자에게 메시지를 보낼 때 사용할 prefix - (주민)추가
		config.setUserDestinationPrefix("/user");
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		// 클라이언트가 처음 접속할 Endpoint
		registry.addEndpoint("/starworks-groupware-websocket")
				.setAllowedOriginPatterns("*")
				.addInterceptors(new HttpSessionHandshakeInterceptor());
				//.withSockJS(); // 이 두 개는 세션 기반 사용자 가져오는거 (WebSocket handshake 요청 인증 필요?)
	}
}
