package kr.or.ddit.vertex.ai.component;

import java.util.List;

import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
//@Component
public class SearchTermExtractor {

    private final VertexAiGeminiChatModel chatModel;

    public SearchTermExtractor(VertexAiGeminiChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * 하이브리드 검색어 추출 (패턴 + AI)
     */
    public String extract(String question) {
        log.info("검색어 추출 시작: '{}'", question);

        // 1단계: 패턴 매칭
        String result = extractByPattern(question);

        if (result != null && isValid(result)) {
            log.info("패턴 추출 성공: '{}'", result);
            return result;
        }

        // 2단계: AI 폴백
        log.info("AI 폴백 시작");
        result = extractByAI(question);

        if (result != null && isValid(result)) {
            log.info("AI 추출 성공: '{}'", result);
            return result;
        }

        log.warn("검색어 추출 실패");
        return null;
    }

    private String extractByPattern(String question) {
        // 패턴 1: "XXX팀"
        if (question.matches(".*([가-힣]+팀).*")) {
            return question.replaceAll(".*([가-힣]+팀).*", "");
        }

        // 패턴 2: "XXX 연락처"
        if (question.matches(".*([가-힣]{2,4})(이|가|은|는)?\\s*(연락처|전화|이메일).*")) {
            String name = question.replaceAll(".*([가-힣]{2,4})(이|가|은|는)?\\s*(연락처|전화|이메일).*", "");
            return isValid(name) ? name : null;
        }

        // 불용어 제거 후 재시도
        String cleaned = removeStopwords(question);
        if (cleaned.matches(".*([가-힣]{2,4}).*")) {
            String name = cleaned.replaceAll(".*([가-힣]{2,4}).*", "");
            return isValid(name) ? name : null;
        }

        return null;
    }

    private String extractByAI(String question) {
        try {
            String prompt = String.format("""
                다음 질문에서 사원 이름 또는 부서명만 추출하세요.

                질문: %s

                규칙:
                1. 이름(2-4글자) 또는 부서명(XXX팀)만 추출
                2. 조사 제거
                3. 일반 단어 제외
                4. 없으면 "NONE" 출력
                5. 단어만 출력
                """, question);

            Prompt aiPrompt = new Prompt(List.of(new UserMessage(prompt)));
            ChatResponse response = chatModel.call(aiPrompt);
            String extracted = response.getResult().getOutput().getText().trim()
                .replaceAll("[\"'`]", "");

            return "NONE".equalsIgnoreCase(extracted) ? null : extracted;

        } catch (Exception e) {
            log.error("AI 추출 오류", e);
            return null;
        }
    }

    private String removeStopwords(String text) {
        String[] stopwords = {"누구", "연락처", "전화", "이메일", "알려줘",
            "찾아줘", "검색", "이", "가", "은", "는"};
        String result = text;
        for (String word : stopwords) {
            result = result.replace(word, " ");
        }
        return result.trim().replaceAll("\\s+", " ");
    }

    public boolean isValid(String term) {
        if (term == null || term.length() < 2) {
            return false;
        }

        String[] blacklist = {"회사", "부서", "사원", "직원", "사람", "누구"};
        String lower = term.toLowerCase();

        for (String blocked : blacklist) {
            if (lower.equals(blocked)) {
                return false;
            }
        }

        // 조사로 끝나거나 불완전한 한글 체크
        return !term.matches(".*[을를이가에서]$") &&
               !term.matches(".*[ㄱ-ㅎㅏ-ㅣ].*");
    }
}