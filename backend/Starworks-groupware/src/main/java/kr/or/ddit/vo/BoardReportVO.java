package kr.or.ddit.vo;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class BoardReportVO {
    private Long reportId;
    private String pstId;
    private String reporterId;
    private String reasonText;
    private String statusCd;
    private LocalDateTime crtDt;
}
