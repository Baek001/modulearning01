package kr.or.ddit.vertex.ai.service;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import kr.or.ddit.vertex.ai.service.IntentClassifierService.Intent;
import kr.or.ddit.vo.UsersVO;
import lombok.extern.slf4j.Slf4j;

@Slf4j
//@Service
public class DocumentRAGService {

    private final VectorStoreService vectorStoreService;
    private final EmployeeSearchService employeeSearchService;
    private final AIAnswerGeneratorService answerGenerator;
    private final IntentClassifierService intentClassifier;
    private final VacationApplicationService vacationApplicationService;
    private final NoticeGenerationService noticeGenerationService;
    private final MeetingReservationAIService meetingReservationAIService;
    private final NewMenuDocumentApplicationService newMenuApplicationService;

    public DocumentRAGService(VectorStoreService vectorStoreService,
                                   EmployeeSearchService employeeSearchService,
                                   AIAnswerGeneratorService answerGenerator,
                                   IntentClassifierService intentClassifier,
                                   VacationApplicationService vacationApplicationService,
                                   NoticeGenerationService noticeGenerationService,
    							   MeetingReservationAIService meetingReservationAIService,
    							   NewMenuDocumentApplicationService newMenuDocumentApplicationService) { // 생성자 주입
        this.vectorStoreService = vectorStoreService;
        this.employeeSearchService = employeeSearchService;
        this.answerGenerator = answerGenerator;
        this.intentClassifier = intentClassifier;
        this.vacationApplicationService = vacationApplicationService;
        this.noticeGenerationService = noticeGenerationService;
        this.meetingReservationAIService = meetingReservationAIService;
        this.newMenuApplicationService = newMenuDocumentApplicationService;
    }

    /**
     * 스마트 모드: 자동 판단 후 답변
     */
    public String answerQuestionSmart(String question, UsersVO user, String userTimezone, ZonedDateTime userTime, List<Map<String, String>> history) {
        log.info("Smart 모드 시작: {}", question);

        Intent intent = intentClassifier.classifyIntent(question);
        log.info("의도 분류 결과: {}", intent);

        String userName = (user != null) ? user.getUserNm() : "사용자";
        String userId = (user != null) ? user.getUserId() : "anonymous";

        switch (intent) {
            case SELF_SEARCH:
                return employeeSearchService.getSelfInfo(user, question, history);

            case EMPLOYEE_SEARCH:
                return employeeSearchService.search(question, userId, history);

            case VACATION_APPLICATION:
                return vacationApplicationService.applyVacation(question, user, history);

            case DOCUMENT_SEARCH:
                return answerWithRAG(question, userName, userTimezone, userTime, history);

            case NOTICE_GENERATION:
            	return noticeGenerationService.generateNotice(question, user, history);

            case MEETING_RESERVATION:
            	return meetingReservationAIService.reserve(question, user, history);

            case NEW_MENU_APPLICATION:
                return newMenuApplicationService.applyNewMenu(question, user, history);

            case GENERAL_CONVERSATION:
            default:
                return answerGenerator.generateGeneralAnswer(question, userName, userTimezone, userTime, history);
        }
    }

    /**
     * RAG 기반 답변
     */
    public String answerWithRAG(String question, String userName, String userTimezone, ZonedDateTime userTime, List<Map<String, String>> history) {
        log.info("질문 수신: {}", question);

        List<Document> relevantDocs = vectorStoreService
            .searchSimilarDocuments(question, 5, 0.7);

        if (relevantDocs.isEmpty()) {
            log.info("관련 문서를 찾지 못해 일반 대화로 처리합니다.");
            return answerGenerator.generateGeneralAnswer(question, userName, userTimezone, userTime, history);
        } else {
            log.info("RAG 모드 - {}개 문서 발견", relevantDocs.size());
            return answerGenerator.generateRAGAnswer(question, relevantDocs, history);
        }
    }
}

