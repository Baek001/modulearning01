package kr.or.ddit.vertex.ai.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import kr.or.ddit.users.service.UsersService;
import kr.or.ddit.vertex.ai.component.SearchTermExtractor;
import kr.or.ddit.vo.UsersVO;
import lombok.extern.slf4j.Slf4j;

@Slf4j
//@Service
public class EmployeeSearchService {

    private final UsersService usersService;
    private final SearchTermExtractor searchTermExtractor;
    private final AIAnswerGeneratorService answerGenerator;

    public EmployeeSearchService(UsersService usersService,
                                SearchTermExtractor searchTermExtractor,
                                AIAnswerGeneratorService answerGenerator) {
        this.usersService = usersService;
        this.searchTermExtractor = searchTermExtractor;
        this.answerGenerator = answerGenerator;
    }

    public String search(String question, String userId, List<Map<String, String>> history) {
        log.info("사원 검색 시작: {}", question);

        // 1. 검색어 추출
        String searchTerm = searchTermExtractor.extract(question);

        if (searchTerm == null) {
            return getSearchGuideMessage();
        }

        log.info("검색어: '{}'", searchTerm);

        // 2. DB 검색
        List<UsersVO> users = usersService.searchUsers(searchTerm);

        if (users == null || users.isEmpty()) {
            return getNoResultMessage(searchTerm);
        }

        log.info("검색 성공: {}명", users.size());

        // 3. AI 답변 생성
        String formattedUsers = formatUsers(users);
        return answerGenerator.generateEmployeeAnswer(question, formattedUsers, history);
    }

    public String getSelfInfo(UsersVO currentUser, String question, List<Map<String, String>> history) {
        if (currentUser == null) {
            return "로그인이 필요한 서비스입니다.";
        }

        log.info("내 정보 검색 시작: userId={}", currentUser.getUserId());

        String formattedUser = formatUsers(List.of(currentUser));
        return answerGenerator.generateEmployeeAnswer(question, formattedUser, history);
    }

    private String formatUsers(List<UsersVO> users) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < users.size(); i++) {
            UsersVO user = users.get(i);
            sb.append(String.format("### 사원 %d\n", i + 1));
            sb.append(String.format("- 이름: %s\n", user.getUserNm()));

            if (user.getDeptNm() != null) {
                sb.append(String.format("- 부서: %s\n", user.getDeptNm()));
            }
            if (user.getJbgdNm() != null) {
                sb.append(String.format("- 직급: %s\n", user.getJbgdNm()));
            }

            sb.append(String.format("- 이메일: %s\n", user.getUserEmail()));
            sb.append(String.format("- 연락처: %s\n", user.getUserTelno()));
            sb.append("\n");
        }
        return sb.toString();
    }

    private String getSearchGuideMessage() {
        return """
            구체적인 사원 이름이나 부서명을 입력해주세요.

            [검색 가능한 형태]
            - "김철수 연락처"
            - "개발팀 사람들"
            """;
    }

    private String getNoResultMessage(String searchTerm) {
        return String.format("""
            '%s'(으)로 검색한 사원을 찾을 수 없습니다.

            [확인사항]
            - 이름이 정확한지 확인
            - 재직 중인 사원인지 확인
            """, searchTerm);
    }
}
