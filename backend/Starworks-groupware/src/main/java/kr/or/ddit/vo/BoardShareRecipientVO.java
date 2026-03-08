package kr.or.ddit.vo;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class BoardShareRecipientVO {
    private Long shareId;
    private String pstId;
    private String recipientUserId;
    private LocalDateTime crtDt;
    private String userNm;
    private String deptNm;
    private String shareTypeCd;
    private String shareTargetId;
}
