package kr.or.ddit.vertex.ai.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import kr.or.ddit.security.CustomUserDetails;
import kr.or.ddit.vertex.ai.service.DocumentRAGService;
import kr.or.ddit.vo.UsersVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * AI 챗봇 WebSocket Controller - 실시간 스트리밍 응답 전송
 */
@Slf4j
//@Controller
@RequiredArgsConstructor
public class AIChatbotWebSocketController {

	private final DocumentRAGService ragService;
	private final SimpMessageSendingOperations messagingTemplate;
	private final ObjectMapper objectMapper = new ObjectMapper(); // ObjectMapper 추가

	/**
	 * 사용자 질문 수신 → RAG 처리 → 스트리밍 응답
	 *
	 * @MessageMapping: 클라이언트가 /app/ai.ask로 전송 응답: /topic/ai/{sessionId}로 청크 단위 전송
	 */
	@MessageMapping("/ai.ask")
	public void handleQuestion(Map<String, String> payload, SimpMessageHeaderAccessor headerAccessor,
			Authentication authentication) {
		String question = payload.get("question");
		String sessionId = payload.get("sessionId");
		String userTimezone = payload.get("userTimezone");

		log.info("📨 처리 중인 sessionId: {}", sessionId);

		// ✅ "초기화" 질문이면 그냥 빈 응답
		if ("초기화".equals(question)) {
			log.info("🔄 더미 요청 처리 (세션 초기화)");

			Map<String, Object> message = new HashMap<>();
			message.put("type", "END");
			message.put("fullAnswer", "");
			messagingTemplate.convertAndSend("/topic/ai/" + sessionId, message);

			return; // ← 히스토리 저장 안 함
		}

		ZonedDateTime userTime = ZonedDateTime.now(ZoneId.of("Asia/Seoul"))
				.withZoneSameInstant(ZoneId.of(userTimezone));

		String userIdForHistory = "anonymous";

		UsersVO user = null;
		if (authentication != null) {
			try {
				CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
				user = userDetails.getRealUser();
				userIdForHistory = user.getUserId();
			} catch (Exception e) {
				log.error("사용자 정보 추출 중 오류 발생", e);
			}
		}

		@SuppressWarnings("unchecked")
		List<Map<String, String>> history = (List<Map<String, String>>) headerAccessor.getSessionAttributes()
				.get("ai_chat_history_" + userIdForHistory);
		if (history == null) {
			history = new ArrayList<>();
		}

		log.info("질문 수신: userId={}, timezone={}", (user != null ? user.getUserId() : "anonymous"), userTimezone);

		try {
			sendStreamStart(sessionId, question);

			String answer = ragService.answerQuestionSmart(question, user, userTimezone, userTime, history);

			// ✅ REDIRECT_FORM 처리
			if (answer != null && answer.contains("\"type\":\"REDIRECT_FORM\"")) {
				try {
					Map<String, Object> formData = objectMapper.readValue(answer, Map.class);

					if ("REDIRECT_FORM".equals(formData.get("type"))) {
						log.info("===== REDIRECT_FORM 처리 시작 =====");

						// 1. 나중을 위해 히스토리를 세션에 저장 (이것이 즉시 동기화될 필요는 없음)
						Map<String, String> historyEntry = new HashMap<>();
						historyEntry.put("question", question);
						historyEntry.put("answer", "신메뉴 기안서 작성 중"); // 히스토리에 남을 메시지
						historyEntry.put("type", "REDIRECT_FORM_IMAGE"); // 히스토리용 타입 지정
						historyEntry.put("timestamp", String.valueOf(System.currentTimeMillis()));

						String base64Image = null;
						if (formData.containsKey("menuImage") && formData.get("menuImage") != null) {
							base64Image = (String) formData.get("menuImage");
							historyEntry.put("imageBase64", base64Image);
						}
						history.add(historyEntry);
						headerAccessor.getSessionAttributes().put("ai_chat_history_" + userIdForHistory, history);
						log.info("✅ 리다이렉트 히스토리 세션에 저장 완료. 히스토리 크기: {}", history.size());

						// 2. 클라이언트에 보낼 리다이렉트 메시지 생성 (필요한 정보만 담기)
						Map<String, Object> redirectMessage = new HashMap<>();
						redirectMessage.put("type", "REDIRECT_FORM");
						redirectMessage.put("url", formData.get("url"));
						redirectMessage.put("script", formData.get("script"));
						if (base64Image != null) {
							redirectMessage.put("imageBase64", base64Image);
						}

						// 3. Thread.sleep 없이 메시지 한 번만 전송
						messagingTemplate.convertAndSend("/topic/ai/" + sessionId, redirectMessage);
						log.info("🚀 REDIRECT_FORM 메시지 전송 완료.");

						return; // 처리 종료
					}
				} catch (JsonProcessingException e) {
					log.error("JSON 파싱 오류: {}", e);
					sendError(sessionId, "폼 데이터 처리 중 오류가 발생했습니다.");
					return;
				} catch (Exception e) {
					log.error("REDIRECT_FORM 처리 중 예외 발생", e);
					sendError(sessionId, "폼 처리 중 예기치 않은 오류가 발생했습니다.");
					return;
				}
			}

			// 일반 응답 처리
			sendAnswerInChunks(sessionId, answer);
			sendStreamEnd(sessionId, answer);

			// 대화 히스토리 저장
			Map<String, String> currentExchange = new HashMap<>();
			currentExchange.put("question", question);
			currentExchange.put("answer", answer);
			history.add(currentExchange);

			if (history.size() > 50)
				history.remove(0);
			headerAccessor.getSessionAttributes().put("ai_chat_history_" + userIdForHistory, history);

		} catch (Exception e) {
			log.error("답변 생성 오류", e);
			sendError(sessionId, "답변 생성 중 오류가 발생했습니다.");
		}
	}

	/**
	 * 스트리밍 시작 알림
	 */
	private void sendStreamStart(String sessionId, String question) {
		Map<String, Object> message = new HashMap<>();
		message.put("type", "START");
		message.put("question", question);
		message.put("timestamp", System.currentTimeMillis());

		messagingTemplate.convertAndSend("/topic/ai/" + sessionId, message);
	}

	/**
	 * 답변을 청크 단위로 전송 (타이핑 효과)
	 */
	private void sendAnswerInChunks(String sessionId, String answer) {
		Random random = new Random();

		for (int i = 0; i < answer.length(); i++) {
			char currentChar = answer.charAt(i);
			String chunk = String.valueOf(currentChar);

			Map<String, Object> message = new HashMap<>();
			message.put("type", "CHUNK");
			message.put("chunk", chunk);
			message.put("progress", (double) (i + 1) / answer.length() * 100);

			messagingTemplate.convertAndSend("/topic/ai/" + sessionId, message);

			try {
				int delay = getTypingDelay(currentChar, random);
				Thread.sleep(delay);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
	}

	private int getTypingDelay(char c, Random random) {
		int baseDelay = 10 + random.nextInt(20);

		if (c == ' ') {
			return baseDelay + random.nextInt(20);
		}

		if (c == '.' || c == '!' || c == '?') {
			return baseDelay + 30 + random.nextInt(30);
		}

		if (c == ',' || c == ';' || c == ':') {
			return baseDelay + 20 + random.nextInt(20);
		}

		if (c == '\n') {
			return baseDelay + 30 + random.nextInt(30);
		}

		return baseDelay;
	}

	/**
	 * 스트리밍 완료 알림
	 */
	private void sendStreamEnd(String sessionId, String fullAnswer) {
		Map<String, Object> message = new HashMap<>();
		message.put("type", "END");
		message.put("fullAnswer", fullAnswer);
		message.put("timestamp", System.currentTimeMillis());

		messagingTemplate.convertAndSend("/topic/ai/" + sessionId, message);
	}

	/**
	 * 폼 리다이렉션 메시지 전송
	 */
	private void sendRedirectForm(String sessionId, Map<String, Object> redirectData) {
		messagingTemplate.convertAndSend("/topic/ai/" + sessionId, redirectData);
	}

	/**
	 * 오류 메시지 전송
	 */
	private void sendError(String sessionId, String errorMessage) {
		Map<String, Object> message = new HashMap<>();
		message.put("type", "ERROR");
		message.put("error", errorMessage);
		message.put("timestamp", System.currentTimeMillis());

		messagingTemplate.convertAndSend("/topic/ai/" + sessionId, message);
	}
}