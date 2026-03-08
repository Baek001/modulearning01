package kr.or.ddit.vertex.ai.service;

import java.util.List;

import org.springframework.stereotype.Service;

import kr.or.ddit.users.service.UserHistoryService;
import kr.or.ddit.vo.UserHistoryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import kr.or.ddit.vo.UsersVO;

@Slf4j
//@Service
@RequiredArgsConstructor
public class NoticeGenerationService {

	private final UserHistoryService userHistoryService;
	private final AIAnswerGeneratorService answerGenerator;
	private final VertexAiGeminiChatModel chatModel;
	private final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * 사용자 요청에 따른 공지사항 생성 (대화형)
	 *
	 * @param question 사용자 질문
	 * @param user     현재 로그인 사용자
	 * @param history  대화 히스토리
	 * @return 응답 메시지 또는 생성된 공지사항 JSON
	 */
	public String generateNotice(String question, UsersVO user, List<Map<String, String>> history) {
		if (user == null) {
			return "공지사항 생성은 로그인이 필요한 서비스입니다.";
		}

		log.info("공지사항 생성 요청: userId={}, question={}", user.getUserId(), question);

		try {
			// 1. 전체 인사기록 조회
			List<UserHistoryVO> allHistories = userHistoryService.readUserHistoryList();

			if (allHistories == null || allHistories.isEmpty()) {
				return "생성할 인사기록이 없습니다. 인사발령 데이터를 먼저 등록해주세요.";
			}

			// 2. AI를 통해 필터링 조건 추출
			FilterCriteria criteria = extractFilterCriteria(question);
			log.info("추출된 필터 조건: {}", criteria);

			// 3. 조건이 충분히 명확한지 확인
			if (!isCriteriaSpecific(criteria)) {
				// 조건이 불충분하면 사용자에게 선택지 제시
				return buildCriteriaSelectionMessage(allHistories);
			}

			// 4. 조건에 맞는 인사기록 필터링
			List<UserHistoryVO> filteredHistories = filterHistories(allHistories, criteria);

			if (filteredHistories.isEmpty()) {
				return String.format("'%s' 조건에 해당하는 인사기록을 찾을 수 없습니다.\n\n%s", question,
						buildCriteriaSelectionMessage(allHistories));
			}

			log.info("필터링된 인사기록: {}건", filteredHistories.size());

			// 5. 사용자에게 확인 요청
			return buildConfirmationMessage(filteredHistories, criteria);

		} catch (Exception e) {
			log.error("공지사항 생성 중 오류 발생: {}", e.getMessage(), e);
			return "공지사항 생성 중 오류가 발생했습니다. 관리자에게 문의하세요.";
		}
	}

	/**
	 * 실제 공지사항 생성 (확인 후 실행)
	 */
	public String confirmAndGenerate(String question, UsersVO user, List<Map<String, String>> history) {
		if (user == null) {
			return "공지사항 생성은 로그인이 필요한 서비스입니다.";
		}

		try {
			// 1. 전체 인사기록 조회
			List<UserHistoryVO> allHistories = userHistoryService.readUserHistoryList();

			// 2. 필터링 조건 재추출
			FilterCriteria criteria = extractFilterCriteria(question);

			// 3. 필터링
			List<UserHistoryVO> filteredHistories = filterHistories(allHistories, criteria);

			if (filteredHistories.isEmpty()) {
				return "해당 조건의 인사기록을 찾을 수 없습니다.";
			}

			// 4. 데이터 포맷팅
			String formattedData = formatHistoryData(filteredHistories);

			// 5. AI 공지사항 생성
			String jsonResponse = answerGenerator.generateNoticeAnswer(formattedData);

			// 6. 응답 래핑
			return wrapNoticeResponse(jsonResponse, filteredHistories.size(), criteria);

		} catch (Exception e) {
			log.error("공지사항 생성 중 오류 발생", e);
			return "공지사항 생성 중 오류가 발생했습니다.";
		}
	}

	/**
	 * 조건이 충분히 구체적인지 확인
	 */
	private boolean isCriteriaSpecific(FilterCriteria criteria) {
		// 적어도 하나의 조건이 명확하게 지정되어야 함
		return criteria.getYear() != null || criteria.getMonth() != null || criteria.getStartDate() != null
				|| criteria.getChangeType() != null || (criteria.getMaxCount() != null && criteria.getMaxCount() < 20);
	}

	/**
	 * 조건 선택 메시지 생성
	 */
	private String buildCriteriaSelectionMessage(List<UserHistoryVO> allHistories) {
		StringBuilder sb = new StringBuilder();
		sb.append("어떤 인사기록으로 공지사항을 작성하시겠습니까?\n\n");
		sb.append("[선택 가능한 옵션]\n");
		sb.append("- 기간: \"10월달 인사발령\", \"최근 1개월\", \"9월부터 10월까지\"\n");
		sb.append("- 유형: \"승진\", \"부서이동\", \"부서이동 및 승진\"\n");
		sb.append("- 최근: \"최근 10건\", \"최근 인사이동\"\n\n");

		// 통계 정보 제공 (코드를 텍스트로 변환)
		Map<String, Long> typeCount = allHistories.stream()
				.collect(Collectors.groupingBy(h -> convertChangeTypeCodeToText(h.getChangeType()), // ✅ 변환 적용
						Collectors.counting()));

		sb.append("[현재 등록된 인사기록]\n");
		sb.append(String.format("- 전체: %d건\n", allHistories.size()));
		typeCount.forEach((type, count) -> sb.append(String.format("- %s: %d건\n", type, count)));

		return sb.toString();
	}

	/**
	 * 확인 메시지 생성 (필터링된 결과 미리보기)
	 */
	private String buildConfirmationMessage(List<UserHistoryVO> filteredHistories, FilterCriteria criteria) {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("총 %d건의 인사기록이 조회되었습니다.\n\n", filteredHistories.size()));

		// 미리보기 (최대 5건)
		sb.append("[미리보기]\n");
		int previewCount = Math.min(5, filteredHistories.size());
		for (int i = 0; i < previewCount; i++) {
			UserHistoryVO h = filteredHistories.get(i);
			sb.append(String.format("%d. %s (%s) - %s\n", i + 1, h.getUserNm(), h.getChangeType(), h.getCrtDt()));
		}

		if (filteredHistories.size() > 5) {
			sb.append(String.format("... 외 %d건\n", filteredHistories.size() - 5));
		}

		sb.append("\n이 내용으로 공지사항을 작성하시겠습니까?\n");
		sb.append("(\"네, 작성해줘\" 또는 \"공지 생성\" 이라고 답변해주세요)");

		return sb.toString();
	}

	/**
	 * AI를 통해 사용자 요청에서 필터링 조건 추출
	 */
	private FilterCriteria extractFilterCriteria(String question) throws JsonProcessingException {
		String currentDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
		String currentYear = String.valueOf(LocalDate.now().getYear());
		String currentMonth = String.valueOf(LocalDate.now().getMonthValue());

		String promptString = String.format("""
				사용자의 요청에서 인사기록 필터링 조건을 추출하세요.
				현재 날짜: %s (연도: %s, 월: %s)

				특수 키워드 처리:
				- "전체", "모든", "전부" → maxCount: 999
				- "최근" (숫자 없음) → maxCount: 10

				변경유형 매핑:
				- "승진" → "02"
				- "부서이동", "전보" → "01"
				- "부서이동 및 승진", "승진 전보" → "03"

				추출할 정보:
				- startDate: 시작 날짜 (yyyy-MM-dd 형식)
				- endDate: 종료 날짜 (yyyy-MM-dd 형식)
				- year: 연도 (정수)
				- month: 월 (1~12 정수)
				- changeType: 변경유형 코드 ("01", "02", "03" 중 하나, null 가능)
				- maxCount: 최대 개수 (정수)

				사용자 질문: "%s"

				반드시 순수 JSON만 출력:
				""", currentDate, currentYear, currentMonth, question);

		Prompt prompt = new Prompt(new UserMessage(promptString));
		ChatResponse response = chatModel.call(prompt);
		String json = response.getResult().getOutput().getText().trim();

		log.info("AI 추출 필터 조건 (raw): {}", json);

		// 마크다운 코드 블록 제거 (더 강력한 로직)
		json = removeMarkdownCodeBlock(json);

		log.info("AI 추출 필터 조건 (cleaned): {}", json);

		return objectMapper.readValue(json, FilterCriteria.class);
	}

	/**
	 * 마크다운 코드 블록 제거 (개선된 버전)
	 */
	private String removeMarkdownCodeBlock(String text) {
		if (text == null)
			return null;

		text = text.trim();

		// `````` 패턴 제거
		if (text.startsWith("```")) {
			text = text.substring("```json".length()).trim();
		} else if (text.startsWith("```")) {
			text = text.substring("```".length()).trim();
		}
		if (text.endsWith("```")) {
			text = text.substring(0, text.length() - 3).trim();
		}

		return text;
	}

	// filterHistories, parseDate, formatHistoryData, wrapNoticeResponse는 이전과 동일

	private List<UserHistoryVO> filterHistories(List<UserHistoryVO> allHistories, FilterCriteria criteria) {
		List<UserHistoryVO> filtered = allHistories.stream().filter(h -> {
			if (criteria.getStartDate() != null && h.getCrtDt() != null) {
				LocalDate historyDate = h.getCrtDt().toLocalDate();
				if (historyDate != null && historyDate.isBefore(criteria.getStartDate())) {
					return false;
				}
			}

			if (criteria.getEndDate() != null && h.getCrtDt() != null) {
				LocalDate historyDate = h.getCrtDt().toLocalDate();
				if (historyDate != null && historyDate.isAfter(criteria.getEndDate())) {
					return false;
				}
			}

			if (criteria.getYear() != null && h.getCrtDt() != null) {
				LocalDate historyDate = h.getCrtDt().toLocalDate();
				if (historyDate != null && historyDate.getYear() != criteria.getYear()) {
					return false;
				}
			}

			if (criteria.getMonth() != null && h.getCrtDt() != null) {
				LocalDate historyDate = h.getCrtDt().toLocalDate();
				if (historyDate != null && historyDate.getMonthValue() != criteria.getMonth()) {
					return false;
				}
			}

			if (criteria.getChangeType() != null && h.getChangeType() != null) {
				if (!h.getChangeType().contains(criteria.getChangeType())) {
					return false;
				}
			}

			return true;
		}).collect(Collectors.toList());

		if (criteria.getMaxCount() != null && filtered.size() > criteria.getMaxCount()) {
			filtered = filtered.subList(0, criteria.getMaxCount());
		}

		return filtered;
	}

	private String formatHistoryData(List<UserHistoryVO> histories) {
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < histories.size(); i++) {
			UserHistoryVO h = histories.get(i);
			sb.append(String.format("### 인사발령 %d\n", i + 1));
			sb.append(String.format("- 성명: %s\n", h.getUserNm()));
			sb.append(String.format("- 변경유형: %s\n", convertChangeTypeCodeToText(h.getChangeType()))); // ✅ 변환 적용

			if (h.getBeforeDeptNm() != null || h.getAfterDeptNm() != null) {
				sb.append(String.format("- 부서 변경: %s → %s\n", h.getBeforeDeptNm() != null ? h.getBeforeDeptNm() : "신규",
						h.getAfterDeptNm() != null ? h.getAfterDeptNm() : "퇴사"));
			}

			if (h.getBeforeJbgdNm() != null || h.getAfterJbgdNm() != null) {
				sb.append(String.format("- 직급 변경: %s → %s\n", h.getBeforeJbgdNm() != null ? h.getBeforeJbgdNm() : "신규",
						h.getAfterJbgdNm() != null ? h.getAfterJbgdNm() : "-"));
			}

			if (h.getReason() != null) {
				sb.append(String.format("- 사유: %s\n", h.getReason()));
			}

			sb.append(String.format("- 발령일: %s\n\n", h.getCrtDt()));
		}

		return sb.toString();
	}

	private String wrapNoticeResponse(String jsonResponse, int recordCount, FilterCriteria criteria) {
		try {
			Map<String, Object> noticeData = objectMapper.readValue(jsonResponse, Map.class);

			Map<String, Object> wrappedResponse = new HashMap<>();
			wrappedResponse.put("type", "NOTICE_GENERATED");
			wrappedResponse.put("recordCount", recordCount);
			wrappedResponse.put("filterCriteria", criteria);
			wrappedResponse.put("notice", noticeData);

			return objectMapper.writeValueAsString(wrappedResponse);

		} catch (Exception e) {
			log.error("공지사항 응답 래핑 중 오류", e);
			return jsonResponse;
		}
	}

	private static class FilterCriteria {
		private LocalDate startDate;
		private LocalDate endDate;
		private Integer year;
		private Integer month;
		private String changeType;
		private Integer maxCount;

		public LocalDate getStartDate() {
			return startDate;
		}

		public LocalDate getEndDate() {
			return endDate;
		}

		public Integer getYear() {
			return year;
		}

		public Integer getMonth() {
			return month;
		}

		public String getChangeType() {
			return changeType;
		}

		public Integer getMaxCount() {
			return maxCount;
		}

		@Override
		public String toString() {
			return String.format(
					"FilterCriteria{startDate=%s, endDate=%s, year=%s, month=%s, changeType='%s', maxCount=%s}",
					startDate, endDate, year, month, changeType, maxCount);
		}
	}

	/**
	 * 변경유형 코드를 텍스트로 변환
	 */
	private String convertChangeTypeCodeToText(String changeTypeCode) {
		if (changeTypeCode == null) {
			return "기타";
		}

		switch (changeTypeCode) {
		case "01":
			return "부서이동";
		case "02":
			return "승진";
		case "03":
			return "부서이동 및 승진";
		default:
			return "기타";
		}
	}
}
