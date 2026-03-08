package kr.or.ddit.vo;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class BoardReactionVO {
    private String pstId;
    private String userId;
    private String reactionCd;
    private LocalDateTime crtDt;
    private String userNm;
    private String deptNm;
    private String filePath;
}
