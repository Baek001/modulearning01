package kr.or.ddit.board.workspace.service;

import java.util.List;
import java.util.Map;

import kr.or.ddit.board.workspace.dto.BoardAvailabilityRequest;
import kr.or.ddit.board.workspace.dto.BoardPinRequest;
import kr.or.ddit.board.workspace.dto.BoardPollVoteRequest;
import kr.or.ddit.board.workspace.dto.BoardReportRequest;
import kr.or.ddit.board.workspace.dto.BoardShareRequest;
import kr.or.ddit.board.workspace.dto.BoardTodoStatusRequest;
import kr.or.ddit.vo.BoardVO;

public interface BoardWorkspaceService {

    Map<String, Object> readWorkspace(String userId, boolean admin, Map<String, String> params);

    BoardVO readBoardDetail(String pstId, String userId, boolean admin);

    BoardVO createBoard(BoardVO board, String userId, boolean admin);

    BoardVO updateBoard(String pstId, BoardVO board, String userId, boolean admin);

    void deleteBoard(String pstId, String userId, boolean admin);

    Map<String, Object> toggleLike(String pstId, String userId, boolean admin);

    List<?> readReaders(String pstId, String userId, boolean admin);

    List<?> readLikeUsers(String pstId, String userId, boolean admin);

    Map<String, Object> shareBoard(String pstId, BoardShareRequest request, String userId, boolean admin);

    Map<String, Object> reportBoard(String pstId, BoardReportRequest request, String userId, boolean admin);

    BoardVO pinBoard(String pstId, BoardPinRequest request, String userId, boolean admin);

    BoardVO votePoll(String pstId, BoardPollVoteRequest request, String userId, boolean admin);

    BoardVO updateTodoStatus(String pstId, String assigneeUserId, BoardTodoStatusRequest request, String userId, boolean admin);

    Map<String, Object> readScheduleAvailability(BoardAvailabilityRequest request, String userId);

    int publishDueScheduledPosts();
}
