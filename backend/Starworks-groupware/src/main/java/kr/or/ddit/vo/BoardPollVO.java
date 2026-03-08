package kr.or.ddit.vo;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class BoardPollVO {
    private String pstId;
    private String multipleYn;
    private String anonymousYn;
    private String resultOpenYn;
    private String participantOpenYn;
    private LocalDateTime deadlineDt;
    private Integer totalResponses;
    private Boolean voted;
    private Boolean closed;
    private List<BoardPollOptionVO> options;
}
