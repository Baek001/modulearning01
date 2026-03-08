package kr.or.ddit.vertex.ai.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
//@Service
@RequiredArgsConstructor
public class ImageGenerationService {

	private final ObjectMapper objectMapper;

	@Value("${spring.ai.vertex.ai.gemini.project-id}")
	private String projectId;

	@Value("${spring.ai.vertex.ai.gemini.location}")
	private String location;

	private String cachedAccessToken;
	private long tokenExpirationTime;
	private RestTemplate restTemplate;

	/**
	 * Imagen API를 호출하여 이미지 생성
	 */
	public String generateImage(String prompt) {
		try {
			log.info("이미지 생성 시작: {}", prompt);

			if (restTemplate == null) {
				SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
				factory.setReadTimeout(120000); // 120초(2분)
				factory.setConnectTimeout(10000);  // 10초
				restTemplate = new RestTemplate(factory);
			}

			String url = String.format(
					"https://%s-aiplatform.googleapis.com/v1/projects/%s/locations/%s/publishers/google/models/imagen-4.0-fast-generate-001:predict",
					location, projectId, location);
			log.info("3️⃣ API URL: {}", url);

			Map<String, Object> instanceMap = new HashMap<>();
			instanceMap.put("prompt", prompt); // ✅ 프롬프트를 정확히 전달

			Map<String, Object> parametersMap = new HashMap<>();
			parametersMap.put("sampleCount", 1);
			parametersMap.put("aspectRatio", "1:1");
			parametersMap.put("addWatermark", false);
			parametersMap.put("safetySetting", "block_medium_and_above");

			Map<String, Object> requestBody = new HashMap<>();
			requestBody.put("instances", new Object[] { instanceMap });
			requestBody.put("parameters", parametersMap);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.setBearerAuth(getAccessToken());

			HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);

			log.debug("Imagen API 요청: URL={}, Prompt={}", url, prompt); // ✅ 프롬프트 디버깅

			log.info("4️⃣ Imagen API 호출 중...");
			ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
			log.info("5️⃣ API 응답 받음: status={}", response.getStatusCode());

			if (response.getStatusCode().is2xxSuccessful()) {
				String result = parseImageResponse(response.getBody());
				log.info("✅ ImageGenerationService.generateImage() 완료! 결과: {} bytes",
						result != null ? result.length() : "null"); // ← 끝
				return result;
			} else {
				log.error("❌ API 실패: {}", response.getStatusCode());
				return null;
			}

		} catch (Exception e) {
			log.error("❌ generateImage() 예외 발생!", e); // ← 예외 추가
			return null;
		}
	}

	/**
	 * 응답에서 Base64 이미지 추출 (프리픽스 표준화)
	 */
	private String parseImageResponse(String responseBody) throws Exception {
		@SuppressWarnings("unchecked")
		Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> predictions = (List<Map<String, Object>>) responseMap.get("predictions");

		if (predictions == null || predictions.isEmpty()) {
			log.warn("응답에 predictions가 없습니다");
			return null;
		}

		Map<String, Object> firstPrediction = predictions.get(0);
		String base64Image = (String) firstPrediction.get("bytesBase64Encoded");

		if (base64Image == null) {
			log.warn("bytesBase64Encoded 필드가 없습니다");
			return null;
		}

		// ✅ 프리픽스 표준화
		if (!base64Image.startsWith("data:image/")) {
			base64Image = "data:image/png;base64," + base64Image;
		}

		log.info("이미지 생성 성공: {} bytes", base64Image.length());
		return base64Image;
	}

	/**
	 * Google Cloud 인증 토큰 획득 (캐싱)
	 */
	private String getAccessToken() throws IOException {
		if (cachedAccessToken != null && System.currentTimeMillis() < tokenExpirationTime) {
			return cachedAccessToken;
		}

		GoogleCredentials credentials = GoogleCredentials.getApplicationDefault()
				.createScoped("https://www.googleapis.com/auth/cloud-platform");

		credentials.refresh();

		cachedAccessToken = credentials.getAccessToken().getTokenValue();
		tokenExpirationTime = System.currentTimeMillis() + (59 * 60 * 1000);

		return cachedAccessToken;
	}
}
