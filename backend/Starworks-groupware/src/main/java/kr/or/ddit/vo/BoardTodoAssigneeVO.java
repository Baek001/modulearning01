package kr.or.ddit.vo;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class BoardTodoAssigneeVO {
    private String pstId;
    private String userId;
    private String statusCd;
    private LocalDateTime crtDt;
    private LocalDateTime lastChgDt;
    private String userNm;
    private String deptNm;
    private String jbgdNm;
    private String filePath;
}
