package kr.or.ddit.board.comment.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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

import kr.or.ddit.board.comment.service.BoardCommentService;
import kr.or.ddit.comm.validate.InsertGroupNotDefault;
import kr.or.ddit.vo.BoardCommentVO;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/rest/board-comment/{pstId}")
@RequiredArgsConstructor
public class boardCommentRestController {

    private final BoardCommentService service;
    private final ObjectMapper objectMapper;

    @GetMapping
    public List<BoardCommentVO> readBoardCommentList(@PathVariable String pstId) {
        return service.readBoardCommentList(pstId);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> createBoardCommentJson(
        @Validated(InsertGroupNotDefault.class) @RequestBody BoardCommentVO boardComment,
        @PathVariable String pstId,
        @RequestParam(required = false) String upCmntSqn,
        Authentication authentication
    ) {
        return createBoardCommentInternal(boardComment, List.of(), pstId, upCmntSqn, authentication);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> createBoardCommentMultipart(
        @RequestPart("payload") String payload,
        @RequestPart(name = "files", required = false) List<MultipartFile> files,
        @PathVariable String pstId,
        @RequestParam(required = false) String upCmntSqn,
        Authentication authentication
    ) throws IOException {
        BoardCommentVO boardComment = objectMapper.readValue(payload, BoardCommentVO.class);
        return createBoardCommentInternal(boardComment, files, pstId, upCmntSqn, authentication);
    }

    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateBoardCommentJson(
        @PathVariable String pstId,
        @RequestParam String cmntSqn,
        @RequestBody BoardCommentVO boardComment,
        Authentication authentication
    ) {
        return updateBoardCommentInternal(pstId, cmntSqn, boardComment, List.of(), authentication);
    }

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateBoardCommentMultipart(
        @PathVariable String pstId,
        @RequestParam String cmntSqn,
        @RequestPart("payload") String payload,
        @RequestPart(name = "files", required = false) List<MultipartFile> files,
        Authentication authentication
    ) throws IOException {
        BoardCommentVO boardComment = objectMapper.readValue(payload, BoardCommentVO.class);
        return updateBoardCommentInternal(pstId, cmntSqn, boardComment, files, authentication);
    }

    @DeleteMapping
    public Map<String, Object> removeBoardComment(
        @PathVariable String pstId,
        @RequestParam String cmntSqn,
        Authentication authentication
    ) {
        BoardCommentVO targetComment = new BoardCommentVO();
        targetComment.setCmntSqn(cmntSqn);
        targetComment = service.readBoardCommentDetail(targetComment);

        if (!canManageComment(targetComment, authentication)) {
            return Map.of("success", false, "message", "댓글 삭제 권한이 없습니다.");
        }

        boolean success = service.removeBoardComment(cmntSqn);

        BoardCommentVO boardComment = new BoardCommentVO();
        boardComment.setCmntSqn(cmntSqn);
        boardComment = service.readBoardCommentDetail(boardComment);

        Map<String, Object> respMap = new HashMap<>();
        respMap.put("success", success);
        respMap.put("boardComment", boardComment);
        respMap.put("pstId", pstId);
        return respMap;
    }

    private Map<String, Object> createBoardCommentInternal(
        BoardCommentVO boardComment,
        List<MultipartFile> files,
        String pstId,
        String upCmntSqn,
        Authentication authentication
    ) {
        boardComment.setPstId(pstId);
        boardComment.setUpCmntSqn(upCmntSqn);
        boardComment.setCrtUserId(authentication.getName());
        boardComment.setFileList(files);
        boolean success = service.createBoardComment(boardComment);

        Map<String, Object> respMap = new HashMap<>();
        respMap.put("success", success);
        respMap.put("boardComment", boardComment);
        return respMap;
    }

    private ResponseEntity<?> updateBoardCommentInternal(
        String pstId,
        String cmntSqn,
        BoardCommentVO boardComment,
        List<MultipartFile> files,
        Authentication authentication
    ) {
        BoardCommentVO existingComment = new BoardCommentVO();
        existingComment.setCmntSqn(cmntSqn);
        existingComment = service.readBoardCommentDetail(existingComment);

        if (existingComment == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", "댓글을 찾을 수 없습니다."));
        }

        if (!canManageComment(existingComment, authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "message", "댓글 수정 권한이 없습니다."));
        }

        existingComment.setPstId(pstId);
        existingComment.setContents(boardComment.getContents());
        existingComment.setCrtUserId(authentication.getName());
        existingComment.setFileList(files);

        boolean success = service.modifyBoardComment(existingComment);
        return ResponseEntity.ok(Map.of("success", success, "boardComment", service.readBoardCommentDetail(existingComment)));
    }

    private boolean canManageComment(BoardCommentVO comment, Authentication authentication) {
        if (comment == null || authentication == null) {
            return false;
        }
        if (authentication.getAuthorities() != null && authentication.getAuthorities().stream()
            .anyMatch(authority ->
                "ROLE_ADMIN".equalsIgnoreCase(authority.getAuthority())
                    || "ADMIN".equalsIgnoreCase(authority.getAuthority())
            )) {
            return true;
        }
        return authentication.getName().equals(comment.getCrtUserId());
    }
}
