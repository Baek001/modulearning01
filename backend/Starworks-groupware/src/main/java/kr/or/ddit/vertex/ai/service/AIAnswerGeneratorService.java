package kr.or.ddit.vertex.ai.service;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
//@Service
public class AIAnswerGeneratorService {

    private final VertexAiGeminiChatModel chatModel;

    public AIAnswerGeneratorService(VertexAiGeminiChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * RAG 기반 답변 생성
     */
    public String generateRAGAnswer(String question, List<Document> relevantDocs, List<Map<String, String>> history) {
        String context = relevantDocs.stream().map(Document::getText).collect(Collectors.joining("\n\n"));

        String promptString = String.format("""
                당신은 전문적인 기업 업무 지원 AI입니다.

                === 참고 문서 ===
                %s

                === 질문 ===
                %s

                === 답변 형식 ===
                - 이모티콘 금지
                - 마크다운 금지
                - 카테고리는 대괄호 []
                - 항목은 하이픈(-)
                """, context, question);

        return callAI(promptString, question, history);
    }

    /**
     * 일반 대화 답변 생성
     */
    public String generateGeneralAnswer(String question, String userName, String userTimezone, ZonedDateTime userTime,
            List<Map<String, String>> history) {
        String promptString = String.format("""
                당신은 전문적인 기업 업무 지원 AI입니다. 비서입니다. 주어진 사용자 정보를 활용하여 답변을 개인화하세요.

                === 사용자 정보 ===
                - 이름: %s
                - 시간대: %s
                - 현재 시간: %s

                === 대화 규칙 ===
                - 사용자 정보를 자연스럽게 답변에 녹여내세요. 예를 들어, 시간에 맞춰 아침/점심/저녁 인사를 하거나, 사용자의 시간대에 관련된 이야기를 할 수 있습니다.
                - '%s님, 안녕하세요!' 와 같이 사용자의 이름을 부르며 대화를 시작할 수 있습니다.
                - 항상 친절하고 명랑한 말투를 사용하세요.
                - 이모티콘 금지
                - 마크다운 금지
                - 카테고리는 대괄호 []
                - 항목은 하이픈(-)

                ### 질문
                %s
                """, userName, userTimezone, userTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), userName, question);

        return callAI(promptString, question, history);
    }

    /**
     * 사원 검색 결과 기반 답변 생성
     */
    public String generateEmployeeAnswer(String question, String employeeData, List<Map<String, String>> history) {
        String promptString = String.format("""
                당신은 회사 사원 정보 안내 AI입니다. 아래 DB 조회 결과를 바탕으로 사용자의 질문에 대해 자연스러운 문장으로 답변하세요.

                === DB 조회 결과 ===
                %s

                === 질문 ===
                %s

                === 답변 규칙 ===
                - 요약 문장: '[부서명] [이름] [직급]의 정보입니다.' 와 같이 만드세요.
                - 특정 정보(이메일, 연락처)를 묻는 질문이면, 요약 문장을 '[부서명] [이름] [직급]의 [요청 정보]입니다.' 로 바꾸고, 그 정보만 아래에 목록으로 보여주세요.
                - 사람 이름만으로 검색했다면, 요약 문장 아래에 이메일과 연락처를 목록으로 보여주세요.
                - 중요: 요약 문장에 이미 포함된 이름, 부서, 직급 정보는 아래 목록에 절대 반복해서 표시하지 마세요.
                - 여러 명이 조회된 경우, 각 사람의 정보를 번호 목록으로 구분하여 규칙에 맞게 모두 알려주세요.

                === 예시 1 (특정 정보 요청) ===
                질문: 홍길동 대리 이메일 알려줘
                답변:
                개발팀 홍길동 대리의 이메일입니다.
                - 이메일: gildong.hong@example.com

                === 예시 2 (일반 정보 요청) ===
                질문: 김철수 대리 찾아줘
                답변:
                시스템운영팀 김철수 대리의 정보입니다.
                - 이메일: asdfasdf@modulearning.co.kr
                - 연락처: 010-1234-5678
                """, employeeData, question);

        return callAI(promptString, question, history);
    }

    /**
     * 공지사항 생성 (인사발령 등)
     */
    public String generateNoticeAnswer(String historyData) {
        String promptString = String.format("""
                당신은 회사의 인사담당자입니다. 아래 인사발령 데이터를 바탕으로 공식 공지사항을 작성하세요.

                === 인사발령 데이터 ===
                %s

                === 작성 규칙 ===
                - JSON 형식으로 출력: {"title": "...", "content": "..."}
                - title: "인사발령 안내 (YYYY년 MM월)" 형식
                - content: 마크다운 형식, 표로 정리
                - 구조: 인사말 → 발령내역 표 → 격려문구
                - 공식적이고 정중한 문체

                JSON만 출력하세요:
                """, historyData);

        try {
            Prompt prompt = new Prompt(new UserMessage(promptString));
            ChatResponse response = chatModel.call(prompt);

            // 권장 응답 추출 경로
            String json = response.getResults().get(0).getOutput().getText();
            if (json == null || json.isBlank()) {
                json = response.getResult().getOutput().getText();
            }
            json = json.trim();

            // 마크다운 코드블록 제거
            json = json.replaceAll("(?s)^```","")
                       .replaceAll("(?s)\\s*```\\s*$", "")
                       .trim();

            log.info("공지사항 생성 완료");
            return json;

        } catch (Exception e) {
            log.error("공지사항 생성 오류", e);
            return String.format("""
                    {
                        "title": "인사발령 안내 (%d건)",
                        "content": "공지사항 생성 중 오류가 발생했습니다. 관리자에게 문의하세요."
                    }
                    """, historyData.split("###").length - 1);
        }
    }

    private String callAI(String systemPrompt, String userQuestion, List<Map<String, String>> history) {
        try {
            List<Message> messages = new ArrayList<>();

            messages.add(new SystemMessage(systemPrompt));

            int maxHistory = 5;
            int startIndex = Math.max(0, history.size() - maxHistory);
            List<Map<String, String>> recentHistory = history.subList(startIndex, history.size());

            for (Map<String, String> exchange : recentHistory) {
                messages.add(new UserMessage(exchange.get("question")));
                messages.add(new AssistantMessage(exchange.get("answer")));
            }

            messages.add(new UserMessage(userQuestion));

            Prompt prompt = new Prompt(messages);

            ChatResponse response = chatModel.call(prompt);

            // 권장 응답 추출 경로
            String content = response.getResults().get(0).getOutput().getText();
            if (content == null || content.isBlank()) {
                content = response.getResult().getOutput().getText();
            }

            return content;

        } catch (Exception e) {
            log.error("AI 호출 오류", e);
            return "답변 생성 중 오류가 발생했습니다.";
        }
    }
}
