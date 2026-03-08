package kr.or.ddit.board.workspace.dto;

import java.util.List;

import lombok.Data;

@Data
public class BoardPollVoteRequest {
    private List<Long> optionIds;
}
