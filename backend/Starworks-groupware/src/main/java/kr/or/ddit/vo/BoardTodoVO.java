package kr.or.ddit.vo;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class BoardTodoVO {
    private String pstId;
    private LocalDateTime dueDt;
    private String statusCd;
    private Integer doneCount;
    private Integer totalCount;
    private List<BoardTodoAssigneeVO> assignees;
}
