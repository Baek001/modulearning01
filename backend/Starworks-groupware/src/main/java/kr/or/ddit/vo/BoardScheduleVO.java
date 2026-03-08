package kr.or.ddit.vo;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class BoardScheduleVO {
    private String pstId;
    private LocalDateTime startDt;
    private LocalDateTime endDt;
    private String alldayYn;
    private String repeatRule;
    private String placeText;
    private String placeUrl;
    private Integer reminderMinutes;
    private String videoMeetingYn;
    private String meetingRoomId;
    private Integer meetingReservationId;
    private List<BoardScheduleAttendeeVO> attendees;
}
