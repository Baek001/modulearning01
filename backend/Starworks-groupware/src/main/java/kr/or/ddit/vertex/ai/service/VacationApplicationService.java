package kr.or.ddit.vertex.ai.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import kr.or.ddit.users.service.UsersService;
import kr.or.ddit.vo.UsersVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
//@Service
@RequiredArgsConstructor
public class VacationApplicationService {

    private final UsersService usersService; // 사용자 정보 조회를 위해 필요
    private final VertexAiGeminiChatModel chatModel;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private static final String VACATION_FORM_URL = "/approval/create?formId=ATRZDOC101";

    /**
     * 휴가 신청 로직 처리
     * @param question 사용자 질문
     * @param user 현재 로그인 사용자 정보
     * @return 리다이렉션 URL과 폼 채우기 스크립트를 포함하는 JSON 문자열
     */
    public String applyVacation(String question, UsersVO user, List<Map<String, String>> history) {
        if (user == null) {
            return "휴가 신청은 로그인이 필요한 서비스입니다.";
        }

        try {
            VacationDetails details = extractVacationDetails(question, history);

            // 2. 필수 정보 누락 시 양식 제시
            StringBuilder missingInfo = new StringBuilder();
            if (details.getVacationType() == null) missingInfo.append("- 휴가 종류 (예: 연차, 병가, 경조사휴가)\n");
            if (details.getStartDate() == null) missingInfo.append("- 시작일 (예: 2025-10-28, 내일)\n");
            if (details.getEndDate() == null) missingInfo.append("- 종료일 (예: 2025-10-29, 모레)\n");
            // 사유는 필수가 아니므로 제외

            if (missingInfo.length() > 0) {
                return String.format("휴가 신청을 위해 다음 정보들을 알려주세요:\n%s", missingInfo.toString());
            }

            log.info("폼 채우기 전 사용자 정보 확인 - 이름: {}, 소속: {}, 직책: {}",
                     user.getUserNm(), user.getDeptNm(), user.getJbgdNm());

            // 휴가 사용일 계산
            long durationDays = ChronoUnit.DAYS.between(details.getStartDate(), details.getEndDate()) + 1;
            details.setDurationDays(durationDays);

            // 폼 채우기 스크립트 생성
            String fillScript = generateFillFormScript(user, details);

            // 프론트엔드로 전달할 JSON 응답 생성
            Map<String, String> response = new HashMap<>();
            response.put("type", "REDIRECT_FORM");
            response.put("url", VACATION_FORM_URL);
            response.put("script", fillScript);

            return objectMapper.writeValueAsString(response);

        } catch (Exception e) {
            log.error("휴가 신청 처리 중 오류 발생: {}", e.getMessage(), e); // 예외 메시지 상세 로그
            return "휴가 신청 처리 중 오류가 발생했습니다.";
        }
    }

    /**
     * AI를 통해 휴가 상세 정보 추출
     */
    private VacationDetails extractVacationDetails(String question, List<Map<String, String>> history) throws JsonProcessingException {
        String systemPrompt = String.format("""
            다음 사용자 질문에서 휴가 신청에 필요한 정보(휴가 종류, 시작일, 종료일, 사유)를 JSON 형식으로 추출하세요.
            날짜 정보는 'yyyy-MM-dd' 형식으로 변환하고, 시간 정보는 무시하세요.
            현재 날짜는 %s 입니다. '내일', '모레' 등의 표현은 현재 날짜를 기준으로 계산하세요.
            연도가 명시되지 않은 날짜는 현재 연도(%s)를 사용하세요.
            시작일만 있고 종료일이 없으면 시작일과 동일하게 설정하세요.
            추출할 수 없는 정보는 null로 두세요.
            사용자가 휴가 신청 의사를 명확히 밝혔다고 가정하고 정보를 추출하세요.

            JSON 형식:
            {
              "vacationType": "string", // 예: 연차, 병가, 경조사휴가 등 (반드시 이 예시와 같은 문자열로 추출)
              "startDate": "yyyy-MM-dd",
              "endDate": "yyyy-MM-dd",
              "reason": "string"
            }
            """, LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE), LocalDate.now().getYear());

        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));

        for(Map<String, String> exchange : history) {
        	messages.add(new UserMessage(exchange.get("question")));
        	messages.add(new AssistantMessage(exchange.get("answer")));
        }

        messages.add(new UserMessage(question));

        Prompt aiPrompt = new Prompt(messages);
        ChatResponse response = chatModel.call(aiPrompt);
        String json = response.getResult().getOutput().getText().trim();

        log.info("AI 추출 휴가 상세 정보 (raw): {}", json);

        // AI가 반환한 JSON 문자열에서 마크다운 코드 블록(```json ... ```) 제거
        if (json.startsWith("```json") && json.endsWith("```")) {
            json = json.substring(7, json.length() - 3).trim(); // "```json" (7자)과 "```" (3자) 제거
            log.info("AI 추출 휴가 상세 정보 (cleaned): {}", json);
        }

        try {
            return objectMapper.readValue(json, VacationDetails.class);
        } catch (JsonProcessingException e) {
            log.error("AI가 반환한 JSON 파싱 오류: JSON = '{}'", json, e);
            throw e; // 예외를 다시 던져 상위 catch 블록에서 처리
        }
    }

    /**
     * 폼 자동 채우기 JavaScript 생성
     */
    private String generateFillFormScript(UsersVO user, VacationDetails details) {
        // HTML 구조에 따라 input.form-control의 인덱스를 재확인하고 값 매핑
        // 소속: inputs[2]
        // 직책: inputs[3]
        // 성명: inputs[4]
        // 휴가종류: inputs[5]
        // 휴가기간 시작: inputs[6] (type="date")
        // 휴가기간 종료: inputs[7] (type="date")
        // 휴가 사용일: inputs[8] (type="text")
        // 휴가사유: inputs[9] (type="text")

        String script = String.format("""
            (function() {
                const inputs = document.querySelectorAll('input.form-control');
                if (inputs.length >= 10) { // 최소 10개 (인덱스 9까지) 필요
                    inputs[2].value = '%s'; // 소속
                    inputs[3].value = '%s'; // 직책
                    inputs[4].value = '%s'; // 성명
                    inputs[5].value = '%s'; // 휴가종류
                    inputs[6].value = '%s'; // 휴가기간 시작
                    inputs[7].value = '%s'; // 휴가기간 종료
                    inputs[8].value = '%d'; // 휴가 사용일
                    inputs[9].value = '%s'; // 휴가사유
                } else {
                    console.error('휴가 신청 폼의 input 필드 개수가 예상과 다릅니다. (현재: ' + inputs.length + '개)');
                }
            })();
            """,
            user.getDeptNm() != null ? user.getDeptNm() : "", // 소속
            user.getJbgdNm() != null ? user.getJbgdNm() : "", // 직책
            user.getUserNm() != null ? user.getUserNm() : "", // 성명
            details.getVacationType() != null ? details.getVacationType() : "", // 휴가종류
            details.getStartDate() != null ? details.getStartDate().format(DateTimeFormatter.ISO_LOCAL_DATE) : "", // 휴가기간 시작
            details.getEndDate() != null ? details.getEndDate().format(DateTimeFormatter.ISO_LOCAL_DATE) : "", // 휴가기간 종료
            details.getDurationDays(), // 휴가 사용일
            details.getReason() != null ? details.getReason() : "" // 휴가사유
        );
        return script;
    }

    // 휴가 상세 정보를 담을 내부 클래스 (VO 역할)
    private static class VacationDetails {
        private String vacationType;
        private LocalDate startDate;
        private LocalDate endDate;
        private String reason;
        private long durationDays; // 계산된 휴가 사용일

        // Getters and Setters
        public String getVacationType() { return vacationType; }
        public LocalDate getStartDate() { return startDate; }
        public LocalDate getEndDate() { return endDate; }
        public String getReason() { return reason; }
        public long getDurationDays() { return durationDays; }
        public void setDurationDays(long durationDays) { this.durationDays = durationDays; }
    }
}
