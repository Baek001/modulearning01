package kr.or.ddit.vertex.ai.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import kr.or.ddit.meeting.exception.AlreadyReservationException;
import kr.or.ddit.meeting.exception.RoomInactiveException;
import kr.or.ddit.meeting.service.MeetingReservationService;
import kr.or.ddit.vo.MeetingReservationVO;
import kr.or.ddit.vo.UsersVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
//@Service
@RequiredArgsConstructor
public class MeetingReservationAIService {

	private final VertexAiGeminiChatModel chatModel;
	private final MeetingReservationService meetingReservationService;
	private final ObjectMapper om = new ObjectMapper();

	/**
     * 자연어 → 예약 실행
     * 필수: date(YYYY-MM-DD), startTime(HH:mm), endTime(HH:mm), room(ROOM01~ROOM04 또는 413~416/413호~416호)
     * 선택: title
     */
    public String reserve(String question, UsersVO user, List<Map<String,String>> history) {
        try {
            LocalDate today = LocalDate.now();
            String todayStr = today.toString(); // "2025-10-29" 형식

            // 히스토리 맥락 구성
            String historyContext = "";
            if (history != null && !history.isEmpty()) {
                int startIdx = Math.max(0, history.size() - 5);
                historyContext = history.subList(startIdx, history.size())
                    .stream()
                    .map(h -> "사용자: " + h.get("question") + "\nAI: " + h.get("answer"))
                    .collect(Collectors.joining("\n\n"));

                historyContext = "\n\n=== 이전 대화 맥락 ===\n" + historyContext + "\n";
            }

            String prompt = """
                다음 한국어 요청을 회의실 예약 파라미터(JSON)로 변환하세요.
                반드시 JSON만 출력하고 마크다운/설명 문구 금지.

                스키마:
                {
                  "date": "YYYY-MM-DD",
                  "startTime": "HH:mm",
                  "endTime": "HH:mm",
                  "room": "ROOM01|ROOM02|ROOM03|ROOM04|413|414|415|416|413호|414호|415호|416호",
                  "title": "선택(없으면 빈 문자열)"
                }

                규칙:
                - 시간이 오전/오후 표현이면 24시간 HH:mm으로 변환.
                - "오늘"이면 오늘 날짜(%s)로 변환.
                - "내일"이면 %s로 변환
                - 이전 대화에서 이미 물어본 정보가 있다면 그것을 우선 사용하세요.
                - 필수값 중 하나라도 불명확하면 JSON 대신 정확히 한 문장으로만 물어보세요: "질문: ..."
                %s
                === 현재 사용자 요청 ===
                %s
                """.formatted(todayStr, today.plusDays(1).toString(), historyContext, question); // historyContext 포함

            ChatResponse resp = chatModel.call(new Prompt(new UserMessage(prompt)));

            // 응답 추출 경로
            String content = resp.getResults().get(0).getOutput().getText();
            if (content == null || content.isBlank()) {
                content = resp.getResult().getOutput().getText();
            }
            content = content.trim();

            log.info("AI 응답 원본: {}", content);

            if (content.startsWith("질문:")) return content;

            // ✅ 수정 2: 강화된 코드펜스 제거 (```
            content = content.replaceAll("(?s)^```[a-zA-Z]*\\s*", "")
                             .replaceAll("(?s)\\s*```","")
                             .trim();

            // 추가 안전장치: 첫 '{' 부터 마지막 '}' 까지 추출
            int firstBrace = content.indexOf('{');
            int lastBrace = content.lastIndexOf('}');
            if (firstBrace >= 0 && lastBrace > firstBrace) {
                content = content.substring(firstBrace, lastBrace + 1);
            }

            log.info("JSON 파싱 시도: {}", content);

            JsonNode root = om.readTree(content);
            String date = text(root, "date");
            String start = text(root, "startTime");
            String end = text(root, "endTime");
            String roomRaw = text(root, "room");
            String title = text(root, "title");

            if (isBlank(date) || isBlank(start) || isBlank(end) || isBlank(roomRaw)) {
                return "예약에 필요한 정보가 부족합니다. 날짜(YYYY-MM-DD), 시작/종료 시간(HH:mm), 회의실(413호/ROOM01 등)을 알려주세요.";
            }

            // 시간 HH:mm → HHmm 정수
            int startHm = toHour(start);
            int endHm = toHour(end);
            if (endHm <= startHm) return "종료 시간이 시작 시간보다 같거나 빠릅니다. 올바른 시간 범위를 알려주세요.";

            // 회의실 문자열 → ROOM_ID
            String roomId = toRoomId(roomRaw);
            if (roomId == null) return "회의실을 인식하지 못했습니다. 413호~416호 또는 ROOM01~ROOM04로 알려주세요.";

            // date → LocalDate
            LocalDate meetingDate = LocalDate.parse(date);

            // VO 매핑
            MeetingReservationVO vo = new MeetingReservationVO();
            vo.setRoomId(roomId);
            vo.setMeetingDate(meetingDate);
            vo.setStartTime(startHm);
            vo.setEndTime(endHm);
            vo.setTitle(title == null || title.isBlank() ? "" : title);
            vo.setUserId(user.getUserId()); // 예약자 설정

            MeetingReservationVO created = meetingReservationService.createMeetingReservation(vo, user);

            return "예약 완료: 번호 " + created.getReservationId()
                 + ", 회의실 " + created.getRoomId()
                 + ", 일시 " + date + " " + created.getStartTime() + "시 ~ " + created.getEndTime() + "시"
                 + (isBlank(created.getTitle()) ? "" : ", 제목 \"" + created.getTitle() + "\"");

        } catch (RoomInactiveException e) {
            log.error("회의실 비활성", e);
            return "해당 회의실은 현재 예약이 불가능합니다.";
        } catch (AlreadyReservationException e) {
            log.error("예약 중복", e);
            return "이미 예약된 시간대입니다. 다른 시간 또는 회의실을 선택해주세요.";
        } catch (Exception e) {
            log.error("회의실 예약 AI 처리 오류", e);
            return "회의실 예약 처리 중 오류가 발생했습니다: " + e.getMessage();
        }
    }

	private static String text(JsonNode root, String f) {
		return root.hasNonNull(f) ? root.get(f).asText().trim() : null;
	}

	private static boolean isBlank(String s) {
		return s == null || s.isBlank();
	}

	private static int toHour(String hhmm) {
		LocalTime t = LocalTime.parse(hhmm);
		return t.getHour();
	}

	// "413", "413호", "ROOM01" 등 → ROOM01~ROOM04
	private static String toRoomId(String raw) {
		String s = raw.toUpperCase().replaceAll("\\s+", "");
		// ROOMxx 직입력
		if (s.matches("ROOM0[1-4]"))
			return s;
		// 413호/413/회의실413 등 숫자 추출
		java.util.regex.Matcher m = java.util.regex.Pattern.compile("(41[3-6])").matcher(s);
		if (m.find()) {
			String num = m.group(1);
			switch (num) {
			case "413":
				return "ROOM01";
			case "414":
				return "ROOM02";
			case "415":
				return "ROOM03";
			case "416":
				return "ROOM04";
			}
		}
		return null;
	}
}
