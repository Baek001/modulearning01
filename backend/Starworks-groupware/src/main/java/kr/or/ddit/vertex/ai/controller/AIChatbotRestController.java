package kr.or.ddit.vertex.ai.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;
import kr.or.ddit.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * AI 챗봇 REST API Controller - 대화 히스토리 로드 - 세션 관리
 */
@Slf4j
//@RestController
@RequestMapping("/ai/chatbot")
@RequiredArgsConstructor
public class AIChatbotRestController {
	/**
	 * 대화 히스토리 조회 세션에 저장된 이전 대화 내용 반환
	 */
	@GetMapping("/history")
	public ResponseEntity<Map<String, Object>> getChatHistory(HttpSession session, Authentication authentication) { // ✅
		String userId = "anonymous";

		// ✅ 방법 1: Authentication에서 가져오기 (권장)
		if (authentication != null) {
			try {
				CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
				userId = userDetails.getRealUser().getUserId();
				log.info("인증된 사용자: {}", userId);
			} catch (Exception e) {
				log.warn("사용자 정보 추출 실패, anonymous 사용");
			}
		}

		// ✅ 방법 2: 세션에서 가져오기 (fallback)
		if ("anonymous".equals(userId)) {
			String sessionUserId = (String) session.getAttribute("userId");
			if (sessionUserId != null) {
				userId = sessionUserId;
			}
		}

		@SuppressWarnings("unchecked")
		List<Map<String, String>> history = (List<Map<String, String>>) session
				.getAttribute("ai_chat_history_" + userId);

		if (history == null) {
			history = new ArrayList<>();
		}

		Map<String, Object> response = new HashMap<>();
		response.put("success", true);
		response.put("history", history);
		response.put("userId", userId);

		log.info("히스토리 조회: userId={}, 메시지 개수={}", userId, history.size());

		return ResponseEntity.ok(response);
	}

	/**
	 * 대화 히스토리 초기화
	 */
	@PostMapping("/history/clear")
	public ResponseEntity<Map<String, Object>> clearHistory(HttpSession session) {
		String userId = (String) session.getAttribute("userId");

		if (userId == null) {
			userId = "anonymous";
		}

		session.removeAttribute("ai_chat_history_" + userId);

		Map<String, Object> response = new HashMap<>();
		response.put("success", true);
		response.put("message", "대화 히스토리가 초기화되었습니다.");

		log.info("AI 챗봇 히스토리 초기화: userId={}", userId);

		return ResponseEntity.ok(response);
	}

	/**
	 * 대화 저장 (선택적) WebSocket으로 전송된 메시지를 세션에 저장
	 */
	@PostMapping("/history/save")
	public ResponseEntity<Map<String, Object>> saveMessage(
	        @RequestBody Map<String, String> payload,
	        HttpSession session,
	        Authentication authentication) {  // ✅ Authentication 추가

	    String userId = "anonymous";

	    // ✅ 인증 정보에서 userId 가져오기
	    if (authentication != null) {
	        try {
	            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
	            userId = userDetails.getRealUser().getUserId();
	        } catch (Exception e) {
	            log.warn("사용자 정보 추출 실패");
	        }
	    }

	    String question = payload.get("question");
	    String answer = payload.get("answer");

	    @SuppressWarnings("unchecked")
	    List<Map<String, String>> history = (List<Map<String, String>>)
	        session.getAttribute("ai_chat_history_" + userId);

	    if (history == null) {
	        history = new ArrayList<>();
	    }

	    Map<String, String> message = new HashMap<>();
	    message.put("question", question);
	    message.put("answer", answer);
	    message.put("timestamp", String.valueOf(System.currentTimeMillis()));
	    history.add(message);

	    if (history.size() > 50) {
	        history.remove(0);
	    }

	    session.setAttribute("ai_chat_history_" + userId, history);

	    log.info("메시지 저장 완료: userId={}, 히스토리 크기={}", userId, history.size());

	    Map<String, Object> response = new HashMap<>();
	    response.put("success", true);

	    return ResponseEntity.ok(response);
	}
}
