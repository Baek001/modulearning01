package kr.or.ddit.vo;

import lombok.Data;

@Data
public class BoardScheduleAttendeeVO {
    private String pstId;
    private String userId;
    private String attendanceSttsCd;
    private String userNm;
    private String deptNm;
    private String filePath;
}
