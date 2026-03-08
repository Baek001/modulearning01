package kr.or.ddit.vertex.ai.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import kr.or.ddit.vo.UsersVO;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
//@Service
@RequiredArgsConstructor
public class NewMenuDocumentApplicationService {

	private final VertexAiGeminiChatModel chatModel;
	private final ImageGenerationService imageGenerationService;
	private final ObjectMapper objectMapper = new ObjectMapper()
			.registerModule(new JavaTimeModule())
			.setSerializationInclusion(JsonInclude.Include.NON_NULL);

	private static final String NEW_MENU_FORM_URL = "/approval/create?formId=ATRZDOC104";

	public String applyNewMenu(String question, UsersVO user, List<Map<String, String>> history) {
		if (user == null) {
			return "신메뉴 결재는 로그인이 필요한 서비스입니다.";
		}
		try {
			// 1단계: 고도로 강화된 프롬프트로 사용자 의도를 정확히 JSON으로 추출
			NewMenuDetails details = extractMenuDetailsWithAi(question, history);

			String missingInfo = getMissingInfo(details);
			if (!missingInfo.isEmpty()) {
				return String.format("신메뉴 기안서 작성을 위해 다음 정보들을 알려주세요:\n%s", missingInfo);
			}

			// 2단계: 추출된 JSON 데이터를 바탕으로 이미지 생성용 전문 키워드를 생성하는 AI 호출
			String imageKeywords = generateImageKeywordsWithAi(details);
			String finalImagePrompt = imageKeywords + ", professional food photography, cafe menu style, hyper-realistic, high-detail, studio lighting, clean white background, minimalist, elegant plating, 8k";
			log.info("✅ 최종 생성 이미지 프롬프트: {}", finalImagePrompt);

			String menuImageBase64 = generateImageWithRetries(finalImagePrompt, 3);
			if (menuImageBase64 != null) {
				details.setMenuImageBase64(menuImageBase64);
			}

			// 3단계: 최종 데이터를 기반으로 폼 채우기 스크립트 생성
			String fillScript = generateFillFormScript(user, details);

			Map<String, String> response = new HashMap<>();
			response.put("type", "REDIRECT_FORM");
			response.put("url", NEW_MENU_FORM_URL);
			response.put("script", fillScript);
			if (details.getMenuImageBase64() != null) {
				response.put("menuImage", details.getMenuImageBase64());
			}

			return objectMapper.writeValueAsString(response);

		} catch (Exception e) {
			log.error("신메뉴 기안서 처리 중 최상위 오류 발생", e);
			return "죄송합니다. 요청 처리 중 예상치 못한 오류가 발생했습니다. 다시 시도해주세요.";
		}
	}

	/**
	 * ✅ [1단계 AI] 사용자 의도 추출용 시스템 프롬프트 (줄바꿈 처리 명시)
	 */
	private NewMenuDetails extractMenuDetailsWithAi(String question, List<Map<String, String>> history) throws JsonProcessingException {
		String systemPrompt = String.format("""
			You are a master data extraction AI. Your task is to analyze the user's request and convert it into a structured JSON object.
			### CRITICAL INSTRUCTIONS
			1. Your output MUST be a single, raw JSON object. Do not include any explanations or markdown wrappers like ```
			2. For multi-line fields like 'ingredientRecipe' and 'marketingPlan', YOU MUST ESCAPE ALL NEWLINE CHARACTERS AS \\n to ensure the final output is a valid single-line JSON string.
			3. Based on the current date '%s', accurately calculate relative dates.
			4. If a field's value is not found in the text, it must be null in the JSON.

			### JSON Schema
			Provide the output in this exact JSON structure including all fields:
			{"menuName":...,"category":...,"size":...,"price":...,"costRate":...,"ingredientRecipe":...,"marketingPlan":...,"releaseDate":...,"temperature":...}
			""", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

		List<Message> messages = new ArrayList<>();
		messages.add(new SystemMessage(systemPrompt));
		history.forEach(exchange -> {
			messages.add(new UserMessage(exchange.get("question")));
			messages.add(new AssistantMessage(exchange.get("answer")));
		});
		messages.add(new UserMessage(question));

		ChatResponse response = chatModel.call(new Prompt(messages));
		String rawJson = response.getResult().getOutput().getText().trim();
		log.info("✅ 1단계 AI (정보추출) 응답: {}", rawJson);

		try {
			return objectMapper.readValue(rawJson, NewMenuDetails.class);
		} catch (JsonProcessingException e) {
			log.error("JSON 파싱 실패: {}. 빈 객체를 반환합니다.", e.getMessage());
			return new NewMenuDetails();
		}
	}

	/**
	 * [2단계 AI] 이미지 생성용 키워드 추출 AI (기존 유지)
	 */
	private String generateImageKeywordsWithAi(NewMenuDetails details) throws JsonProcessingException {
		String detailsJson = objectMapper.writeValueAsString(details);
		String systemPrompt = """
			You are an expert prompt engineer for a text-to-image AI.
			Your task is to convert the given JSON data of a cafe menu into a list of descriptive English keywords.
			Your output MUST be a comma-separated list of keywords. No explanations.
			Example 1: {"menuName":"딸기 라떼","temperature":"cold"} -> strawberry latte, pink beverage, milky, iced, cold, in a clear tall glass
			Example 2: {"menuName":"핫초코","temperature":"hot"} -> hot chocolate, rich dark chocolate, steaming, in a white ceramic mug
			""";

		ChatResponse response = chatModel.call(new Prompt(List.of(new SystemMessage(systemPrompt), new UserMessage(detailsJson))));
		String keywords = response.getResult().getOutput().getText().trim();
		log.info("✅ 2단계 AI (키워드 생성) 응답: {}", keywords);
		return keywords;
	}

	/**
	 * 폼 채우기 스크립트 생성 (ID로만 검색, 안정성 강화)
	 */
	private String generateFillFormScript(UsersVO user, NewMenuDetails details) {
		String scriptTemplate = """
			(function() {
			    function fillForm() {
			        const fields = {
			            'menuName': '%s', 'releaseDate': '%s', 'size': '%s',
			            'price': '%s', 'costRate': '%s', 'ingredientRecipe': '%s', 'marketingPlan': '%s', 'category': '%s'
			        };
			        for (const [key, value] of Object.entries(fields)) {
			            if (!value) continue;
			            const el = document.getElementById(key);
			            if (el) { el.value = value; }
			        }
			        const imageBase64 = sessionStorage.getItem('aiFillFormImage') || localStorage.getItem('aiFillFormImage');
			        if (imageBase64) {
			            try {
			                const byteCharacters = atob(imageBase64.split(',')[1] || imageBase64);
			                const byteNumbers = new Array(byteCharacters.length);
			                for (let i = 0; i < byteCharacters.length; i++) byteNumbers[i] = byteCharacters.charCodeAt(i);
			                const byteArray = new Uint8Array(byteNumbers);
			                const blob = new Blob([byteArray], {type: 'image/png'});
			                const file = new File([blob], '%s_menu.png', {type: 'image/png'});
			                const fileInput = document.querySelector('input[type="file"][name="fileList"]');
			                if (fileInput) {
			                    const dataTransfer = new DataTransfer();
			                    dataTransfer.items.add(file);
			                    fileInput.files = dataTransfer.files;
			                    fileInput.dispatchEvent(new Event('change', { bubbles: true }));
			                    sessionStorage.removeItem('aiFillFormImage');
			                    localStorage.removeItem('aiFillFormImage');
			                }
			            } catch(e) { console.error("Image upload script failed:", e); }
			        }
			    }
			    if (document.readyState === 'loading') {
			        document.addEventListener('DOMContentLoaded', fillForm);
			    } else {
			        fillForm();
			    }
			})();
		""";
		return String.format(scriptTemplate,
			details.getMenuName() != null ? escapeJs(details.getMenuName()) : "",
			details.getReleaseDate() != null ? details.getReleaseDate().format(DateTimeFormatter.ISO_LOCAL_DATE) : "",
			details.getSize() != null ? escapeJs(details.getSize()) : "",
			details.getPrice() != null ? details.getPrice().toString() : "",
			details.getCostRate() != null ? details.getCostRate().toString() : "",
			details.getIngredientRecipe() != null ? escapeJs(details.getIngredientRecipe()) : "",
			details.getMarketingPlan() != null ? escapeJs(details.getMarketingPlan()) : "",
			details.getCategory() != null ? escapeJs(details.getCategory()) : "",
			details.getMenuName() != null ? escapeJs(details.getMenuName()) : "menu"
		);
	}

	private String getMissingInfo(NewMenuDetails details) {
		StringBuilder sb = new StringBuilder();
		if (details.getMenuName() == null) sb.append("- 메뉴 이름\n");
		if (details.getCategory() == null) sb.append("- 메뉴 카테고리\n");
		if (details.getPrice() == null) sb.append("- 판매 가격\n");
		if (details.getReleaseDate() == null) sb.append("- 출시 예정일\n");
		return sb.toString();
	}

	private String generateImageWithRetries(String prompt, int maxRetries) {
		for (int i = 0; i < maxRetries; i++) {
			try {
				String imageBase64 = imageGenerationService.generateImage(prompt);
				if (imageBase64 != null && !imageBase64.isEmpty()) return imageBase64;
			} catch (Exception e) {
				log.error("❌ 이미지 생성 예외 (시도 {}): {}", i + 1, e.getMessage());
			}
		}
		return null;
	}

	private String escapeJs(String value) {
		if (value == null) return "";
		return value.replace("\\", "\\\\").replace("'", "\\'").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
	}

	@Data
	private static class NewMenuDetails {
		private String menuName;
		private String category;
		private String size;
		private Integer price;
		private Double costRate;
		private String ingredientRecipe;
		private String marketingPlan;
		private LocalDate releaseDate;
		private String temperature;
		private String menuImageBase64;
	}
}
