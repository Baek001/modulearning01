package kr.or.ddit.vo;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class BoardMentionVO {
    private String pstId;
    private String cmntSqn;
    private String userId;
    private LocalDateTime crtDt;
    private String userNm;
}
