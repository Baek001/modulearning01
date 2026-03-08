package kr.or.ddit.board.workspace.dto;

import java.util.List;

import lombok.Data;

@Data
public class BoardShareRequest {
    private List<String> userIds;
    private List<Long> communityIds;
}
