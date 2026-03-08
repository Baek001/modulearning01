package kr.or.ddit.vertex.ai.service;

import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
//@Service
public class IntentClassifierService {

    private final VertexAiGeminiChatModel chatModel;

    public IntentClassifierService(VertexAiGeminiChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public enum Intent {
        EMPLOYEE_SEARCH,
        DOCUMENT_SEARCH,
        GENERAL_CONVERSATION,
        SELF_SEARCH,
        VACATION_APPLICATION,
        NOTICE_GENERATION,
        MEETING_RESERVATION,
        MEETING_SUMMARIZING,
        IMAGE_GENERATION,
        NEW_MENU_APPLICATION,
    }

    public Intent classifyIntent(String question) {
        // 먼저 키워드 기반 빠른 분류
        Intent quickClassify = quickClassifyByKeywords(question);
        if (quickClassify != Intent.GENERAL_CONVERSATION) {
            log.info("키워드 기반 빠른 분류: {} (질문: {})", quickClassify, question);
            return quickClassify;
        }

        // AI 호출
        String promptString = String.format("""
            사용자 질문의 의도를 다음 카테고리 중 정확히 하나로 분류하세요.
            허용된 카테고리: EMPLOYEE_SEARCH, DOCUMENT_SEARCH, SELF_SEARCH, VACATION_APPLICATION, NOTICE_GENERATION, MEETING_RESERVATION, MEETING_SUMMARIZING, GENERAL_CONVERSATION, NEW_MENU_APPLICATION

            - EMPLOYEE_SEARCH: 다른 사람/부서 정보 검색
            - DOCUMENT_SEARCH: 회사 정책/규정 등 문서 검색
            - SELF_SEARCH: 사용자 본인 정보 검색
            - VACATION_APPLICATION: 휴가 신청 (예: '휴가 신청해줘', '연차 쓰고 싶어')
            - NOTICE_GENERATION: 공지사항 생성 (인사발령, 공지문 작성 등)
            - MEETING_RESERVATION: 회의실 예약 (예: '회의실 예약', '413호 예약', '회의 잡아줘')
            - MEETING_SUMMARIZING: 회의 내용 요약 (회의 전 문서 + 회의 후 메모)
            - NEW_MENU_APPLICATION: 신메뉴 신청 (예: "새 메뉴 추가", "메뉴 기안", "아이스 라떼 메뉴 신청", "신메뉴 추가해줘")
            - GENERAL_CONVERSATION: 위 외 일반 대화

            질문: "%s"

            위 카테고리 이름만 정확히 하나 출력하세요 (설명 금지):
            """, question);

        try {
            Prompt prompt = new Prompt(new UserMessage(promptString));
            ChatResponse response = chatModel.call(prompt);

            String raw = response.getResults().get(0).getOutput().getText();
            if (raw == null || raw.isBlank()) {
                raw = response.getResult().getOutput().getText();
            }

            String classification = raw.trim()
                                       .toUpperCase()
                                       .replaceAll("[^A-Z_]", "");

            log.info("의도 분류 결과: {} (원본: {})", classification, raw);

            for (Intent intent : Intent.values()) {
                if (intent.name().equals(classification)) {
                    return intent;
                }
            }

            // 매칭 실패 시 키워드 휴리스틱 보강
            String lower = question.toLowerCase();

            // 회의실 예약
            if (lower.contains("회의실") || lower.contains("예약") || lower.contains("room") ||
                lower.matches(".*\\d{3}호.*")) {
                log.info("키워드 기반 회의실 예약 분류");
                return Intent.MEETING_RESERVATION;
            }

            // 휴가 신청
            if (lower.contains("휴가") || lower.contains("연차")) {
                return Intent.VACATION_APPLICATION;
            }

            // 직원 검색
            if (lower.contains("직원") || lower.contains("사원") || lower.contains("부서")) {
                return Intent.EMPLOYEE_SEARCH;
            }

            // 신메뉴 신청
            if (lower.contains("메뉴 기안") || lower.contains("메뉴 신청") ||
                lower.contains("메뉴 추가") || lower.contains("신메뉴") ||
                lower.contains("메뉴 제안") || lower.contains("메뉴 신청") ||
                lower.contains("기안하고") || lower.contains("신청하고")) {
                log.info("키워드 기반 신메뉴 신청 분류");
                return Intent.NEW_MENU_APPLICATION;
            }

            log.warn("분류 실패, 일반 대화로 처리: {}", raw);
            return Intent.GENERAL_CONVERSATION;

        } catch (Exception e) {
            log.error("의도 분류 중 오류 발생", e);
            return Intent.GENERAL_CONVERSATION;
        }
    }

    /**
     * ✅ 키워드 기반 빠른 분류 (AI 호출 전)
     */
    private Intent quickClassifyByKeywords(String question) {
        String lower = question.toLowerCase();

        // 회의실 예약
        if (lower.contains("회의실") || lower.contains("예약") || lower.contains("room") ||
            lower.matches(".*\\d{3}호.*")) {
            return Intent.MEETING_RESERVATION;
        }

        // 휴가 신청
        if (lower.contains("휴가") || lower.contains("연차") || lower.contains("병가")) {
            return Intent.VACATION_APPLICATION;
        }

        // 신메뉴 신청
        if (lower.contains("메뉴 기안") || lower.contains("메뉴 신청") ||
            lower.contains("메뉴 추가") || lower.contains("신메뉴") ||
            lower.contains("메뉴 제안") || lower.contains("기안") ||
            lower.contains("라떼") || lower.contains("커피") ||
            lower.contains("디저트") || lower.contains("음료")) {
            return Intent.NEW_MENU_APPLICATION;
        }

        // 직원 검색
        if (lower.contains("직원") || lower.contains("사원") || lower.contains("부서")) {
            return Intent.EMPLOYEE_SEARCH;
        }

        // 공지사항
        if (lower.contains("공지") || lower.contains("발령") || lower.contains("공지사항")) {
            return Intent.NOTICE_GENERATION;
        }

        return Intent.GENERAL_CONVERSATION; // 키워드 없으면 AI에게 맡김
    }
}
