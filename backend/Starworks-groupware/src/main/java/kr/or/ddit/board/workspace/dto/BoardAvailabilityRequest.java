package kr.or.ddit.board.workspace.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class BoardAvailabilityRequest {
    private LocalDateTime startDt;
    private LocalDateTime endDt;
    private List<String> userIds;
}
