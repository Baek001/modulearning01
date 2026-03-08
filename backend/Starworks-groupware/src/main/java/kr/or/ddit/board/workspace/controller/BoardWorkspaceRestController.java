package kr.or.ddit.board.workspace.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import kr.or.ddit.board.workspace.dto.BoardAvailabilityRequest;
import kr.or.ddit.board.workspace.dto.BoardPinRequest;
import kr.or.ddit.board.workspace.dto.BoardPollVoteRequest;
import kr.or.ddit.board.workspace.dto.BoardReportRequest;
import kr.or.ddit.board.workspace.dto.BoardShareRequest;
import kr.or.ddit.board.workspace.dto.BoardTodoStatusRequest;
import kr.or.ddit.board.workspace.service.BoardWorkspaceService;
import kr.or.ddit.vo.BoardVO;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/rest/boards")
@RequiredArgsConstructor
public class BoardWorkspaceRestController {

    private final BoardWorkspaceService boardWorkspaceService;
    private final ObjectMapper objectMapper;

    @GetMapping
    public Map<String, Object> readWorkspace(
        @RequestParam Map<String, String> params,
        Authentication authentication
    ) {
        return boardWorkspaceService.readWorkspace(authentication.getName(), isAdmin(authentication), params);
    }

    @GetMapping("/{pstId}")
    public BoardVO readBoardDetail(@PathVariable String pstId, Authentication authentication) {
        return boardWorkspaceService.readBoardDetail(pstId, authentication.getName(), isAdmin(authentication));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createBoardJson(@RequestBody BoardVO board, Authentication authentication) {
        BoardVO saved = boardWorkspaceService.createBoard(board, authentication.getName(), isAdmin(authentication));
        return ResponseEntity.ok(Map.of("success", true, "board", saved));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createBoardMultipart(
        @RequestPart("payload") String payload,
        @RequestPart(name = "files", required = false) List<MultipartFile> files,
        Authentication authentication
    ) throws IOException {
        BoardVO board = objectMapper.readValue(payload, BoardVO.class);
        board.setFileList(files);
        BoardVO saved = boardWorkspaceService.createBoard(board, authentication.getName(), isAdmin(authentication));
        return ResponseEntity.ok(Map.of("success", true, "board", saved));
    }

    @PutMapping(value = "/{pstId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateBoardJson(
        @PathVariable String pstId,
        @RequestBody BoardVO board,
        Authentication authentication
    ) {
        BoardVO saved = boardWorkspaceService.updateBoard(pstId, board, authentication.getName(), isAdmin(authentication));
        return ResponseEntity.ok(Map.of("success", true, "board", saved));
    }

    @PutMapping(value = "/{pstId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateBoardMultipart(
        @PathVariable String pstId,
        @RequestPart("payload") String payload,
        @RequestPart(name = "files", required = false) List<MultipartFile> files,
        Authentication authentication
    ) throws IOException {
        BoardVO board = objectMapper.readValue(payload, BoardVO.class);
        board.setFileList(files);
        BoardVO saved = boardWorkspaceService.updateBoard(pstId, board, authentication.getName(), isAdmin(authentication));
        return ResponseEntity.ok(Map.of("success", true, "board", saved));
    }

    @DeleteMapping("/{pstId}")
    public Map<String, Object> deleteBoard(@PathVariable String pstId, Authentication authentication) {
        boardWorkspaceService.deleteBoard(pstId, authentication.getName(), isAdmin(authentication));
        return Map.of("success", true);
    }

    @PostMapping("/{pstId}/likes")
    public Map<String, Object> toggleLike(@PathVariable String pstId, Authentication authentication) {
        return boardWorkspaceService.toggleLike(pstId, authentication.getName(), isAdmin(authentication));
    }

    @GetMapping("/{pstId}/likes")
    public List<?> readLikeUsers(@PathVariable String pstId, Authentication authentication) {
        return boardWorkspaceService.readLikeUsers(pstId, authentication.getName(), isAdmin(authentication));
    }

    @GetMapping("/{pstId}/readers")
    public List<?> readReaders(@PathVariable String pstId, Authentication authentication) {
        return boardWorkspaceService.readReaders(pstId, authentication.getName(), isAdmin(authentication));
    }

    @PostMapping("/{pstId}/share")
    public Map<String, Object> shareBoard(
        @PathVariable String pstId,
        @RequestBody BoardShareRequest request,
        Authentication authentication
    ) {
        return boardWorkspaceService.shareBoard(pstId, request, authentication.getName(), isAdmin(authentication));
    }

    @PostMapping("/{pstId}/report")
    public Map<String, Object> reportBoard(
        @PathVariable String pstId,
        @RequestBody BoardReportRequest request,
        Authentication authentication
    ) {
        return boardWorkspaceService.reportBoard(pstId, request, authentication.getName(), isAdmin(authentication));
    }

    @PostMapping("/{pstId}/pin")
    public BoardVO pinBoard(
        @PathVariable String pstId,
        @RequestBody(required = false) BoardPinRequest request,
        Authentication authentication
    ) {
        return boardWorkspaceService.pinBoard(pstId, request, authentication.getName(), isAdmin(authentication));
    }

    @PostMapping("/{pstId}/poll/vote")
    public BoardVO votePoll(
        @PathVariable String pstId,
        @RequestBody BoardPollVoteRequest request,
        Authentication authentication
    ) {
        return boardWorkspaceService.votePoll(pstId, request, authentication.getName(), isAdmin(authentication));
    }

    @PatchMapping("/{pstId}/todo-assignees/{assigneeUserId}")
    public BoardVO updateTodoStatus(
        @PathVariable String pstId,
        @PathVariable String assigneeUserId,
        @RequestBody BoardTodoStatusRequest request,
        Authentication authentication
    ) {
        return boardWorkspaceService.updateTodoStatus(pstId, assigneeUserId, request, authentication.getName(), isAdmin(authentication));
    }

    @PostMapping("/schedule/availability")
    public Map<String, Object> readScheduleAvailability(
        @RequestBody BoardAvailabilityRequest request,
        Authentication authentication
    ) {
        return boardWorkspaceService.readScheduleAvailability(request, authentication.getName());
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null
            && authentication.getAuthorities() != null
            && authentication.getAuthorities().stream()
                .anyMatch(authority ->
                    "ROLE_ADMIN".equalsIgnoreCase(authority.getAuthority())
                    || "ADMIN".equalsIgnoreCase(authority.getAuthority())
                );
    }
}
