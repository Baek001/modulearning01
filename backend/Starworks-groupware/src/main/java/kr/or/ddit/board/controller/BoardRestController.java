package kr.or.ddit.board.controller;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import kr.or.ddit.board.community.service.BoardService;
import kr.or.ddit.vo.BoardVO;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class BoardRestController {

    private final BoardService service;

    @GetMapping("/rest/board-notice")
    public List<BoardVO> readNoticeListNonPaging() {
        return service.readNoticeList(null);
    }

    @GetMapping("/rest/board-notice/dashBoard")
    public List<BoardVO> readNotice() {
        return service.readNotices();
    }

    @GetMapping("/rest/board-community")
    public List<BoardVO> readCommunityListNonPaging(
        @RequestParam(name = "bbsCtgrCd", required = false) String bbsCtgrCd
    ) {
        return service.readCommunityListNonPaging(bbsCtgrCd);
    }

    @GetMapping("/rest/board-category-counts")
    public Map<String, Integer> readCategoryCounts() {
        return service.getCategoryCounts();
    }

    @GetMapping("/rest/board/{pstId}")
    public BoardVO readBoard(@PathVariable String pstId) {
        return service.readBoard(pstId);
    }

    @PostMapping("/rest/board")
    public ResponseEntity<?> createBoard(@RequestBody BoardVO board, Authentication authentication) {
        if (!StringUtils.hasText(board.getBbsCtgrCd())
            || !StringUtils.hasText(board.getPstTtl())
            || !StringUtils.hasText(board.getContents())) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "게시글 제목, 내용, 분류는 필수입니다."
            ));
        }

        if ("F101".equals(board.getBbsCtgrCd()) && !isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "success", false,
                "message", "공지사항은 관리자만 작성할 수 있습니다."
            ));
        }

        board.setCrtUserId(authentication.getName());
        board.setFixedYn(StringUtils.hasText(board.getFixedYn()) ? board.getFixedYn() : "N");
        board.setFileList(Collections.emptyList());

        boolean success = service.createBoard(board);
        if (!success) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "게시글 등록에 실패했습니다."
            ));
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "success", true,
            "board", board
        ));
    }

    @PutMapping("/rest/board/{pstId}")
    public ResponseEntity<?> updateBoard(
        @PathVariable String pstId,
        @RequestBody BoardVO board,
        Authentication authentication
    ) {
        BoardVO existingBoard = service.readBoard(pstId);
        if (!canManageBoard(existingBoard, authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "success", false,
                "message", "수정 권한이 없습니다."
            ));
        }

        if ("F101".equals(board.getBbsCtgrCd()) && !isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "success", false,
                "message", "공지사항은 관리자만 수정할 수 있습니다."
            ));
        }

        board.setPstId(pstId);
        board.setCrtUserId(existingBoard.getCrtUserId());
        board.setLastChgUserId(authentication.getName());
        board.setFixedYn(StringUtils.hasText(board.getFixedYn()) ? board.getFixedYn() : "N");
        board.setFileList(Collections.emptyList());

        boolean success = service.modifyBoard(board);
        if (!success) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "게시글 수정에 실패했습니다."
            ));
        }

        return ResponseEntity.ok(Map.of(
            "success", true,
            "board", service.readBoard(pstId)
        ));
    }

    @DeleteMapping("/rest/board/{pstId}")
    public ResponseEntity<?> deleteBoard(@PathVariable String pstId, Authentication authentication) {
        BoardVO existingBoard = service.readBoard(pstId);
        if (!canManageBoard(existingBoard, authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "success", false,
                "message", "삭제 권한이 없습니다."
            ));
        }

        boolean success = service.removeBoard(pstId);
        return ResponseEntity.ok(Map.of(
            "success", success
        ));
    }

    @PutMapping("/rest/board-vct/{pstId}")
    public void modifyViewCnt(@PathVariable String pstId) {
        service.modifyViewCnt(pstId);
    }

    private boolean canManageBoard(BoardVO board, Authentication authentication) {
        if (board == null || authentication == null) {
            return false;
        }

        if (isAdmin(authentication)) {
            return true;
        }

        if ("F101".equals(board.getBbsCtgrCd())) {
            return false;
        }

        return authentication.getName().equals(board.getCrtUserId());
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
