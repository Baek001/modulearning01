package kr.or.ddit.board.workspace.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import kr.or.ddit.board.community.service.BoardService;
import kr.or.ddit.board.workspace.dto.BoardAvailabilityRequest;
import kr.or.ddit.board.workspace.dto.BoardPinRequest;
import kr.or.ddit.board.workspace.dto.BoardPollVoteRequest;
import kr.or.ddit.board.workspace.dto.BoardReportRequest;
import kr.or.ddit.board.workspace.dto.BoardShareRequest;
import kr.or.ddit.board.workspace.dto.BoardTodoStatusRequest;
import kr.or.ddit.comm.exception.EntityNotFoundException;
import kr.or.ddit.comm.file.service.FileDetailService;
import kr.or.ddit.meeting.service.MeetingReservationService;
import kr.or.ddit.mybatis.mapper.BoardCommentMapper;
import kr.or.ddit.mybatis.mapper.BoardMapper;
import kr.or.ddit.vo.BoardCommentVO;
import kr.or.ddit.vo.BoardMentionVO;
import kr.or.ddit.vo.BoardPollOptionVO;
import kr.or.ddit.vo.BoardPollVO;
import kr.or.ddit.vo.BoardReactionVO;
import kr.or.ddit.vo.BoardScheduleAttendeeVO;
import kr.or.ddit.vo.BoardScheduleVO;
import kr.or.ddit.vo.BoardShareRecipientVO;
import kr.or.ddit.vo.BoardTodoAssigneeVO;
import kr.or.ddit.vo.BoardTodoVO;
import kr.or.ddit.vo.BoardVO;
import kr.or.ddit.vo.MeetingReservationVO;
import kr.or.ddit.websocket.service.impl.NotificationServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class BoardWorkspaceServiceImpl implements BoardWorkspaceService {

    private static final int PAGE_SIZE = 12;
    private static final int PIN_LIMIT = 10;
    private static final Set<String> DEFAULT_IMPORTANCE_CODES = Set.of("normal", "important", "urgent", "notice");
    private static final Set<String> SCHEDULE_IMPORTANCE_CODES = Set.of("normal", "important", "urgent", "notice", "congratulation", "condolence", "event");

    private final NamedParameterJdbcTemplate namedJdbc;
    private final BoardService boardService;
    private final BoardMapper boardMapper;
    private final BoardCommentMapper boardCommentMapper;
    private final FileDetailService fileDetailService;
    private final MeetingReservationService meetingReservationService;
    private final NotificationServiceImpl notificationService;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> readWorkspace(String userId, boolean admin, Map<String, String> params) {
        MapSqlParameterSource queryParams = new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("admin", admin);

        int page = parseInt(params.get("page"), 1);
        int offset = Math.max(page - 1, 0) * PAGE_SIZE;
        String sort = StringUtils.hasText(params.get("sort")) ? params.get("sort") : "recent";
        String whereClause = buildListWhereClause(params, queryParams, userId, admin);

        Integer totalRecords = namedJdbc.queryForObject(
            "SELECT COUNT(*) FROM BOARD B WHERE " + whereClause,
            queryParams,
            Integer.class
        );
        int safeTotal = totalRecords == null ? 0 : totalRecords;
        int totalPages = safeTotal == 0 ? 1 : (int) Math.ceil((double) safeTotal / PAGE_SIZE);

        queryParams.addValue("limit", PAGE_SIZE);
        queryParams.addValue("offset", offset);

        String listSql =
            "SELECT B.PST_ID, B.BBS_CTGR_CD, B.COMMUNITY_ID, B.PST_TTL, B.CONTENTS, B.CRT_USER_ID, B.FIXED_YN, " +
            "       B.FRST_CRT_DT, B.PST_FILE_ID, B.VIEW_CNT, B.IMPORTANCE_CD, B.LINK_URL, B.PST_TYPE_CD, " +
            "       B.PUBLISHED_DT, B.PUBLISH_STATE_CD, B.RESERVED_PUBLISH_DT, B.VISIBILITY_CD, B.COMMUNITY_AUTHOR_ROLE_CD, U.USER_NM, " +
            "       D.DEPT_NM, J.JBGD_NM, C.COMMUNITY_NM, C.COMMUNITY_TYPE_CD, " +
            "       COALESCE((SELECT COUNT(*) FROM BOARD_COMMENT BC WHERE BC.PST_ID = B.PST_ID AND COALESCE(BC.DEL_YN, 'N') = 'N'), 0) AS COMMENT_COUNT, " +
            "       COALESCE((SELECT COUNT(*) FROM BOARD_POST_REACTION BR WHERE BR.PST_ID = B.PST_ID AND BR.REACTION_CD = 'like'), 0) AS LIKE_COUNT, " +
            "       COALESCE((SELECT COUNT(*) FROM USER_FEED_READ R WHERE R.PST_ID = B.PST_ID), 0) AS READER_COUNT, " +
            "       COALESCE((SELECT COUNT(*) FROM BOARD_SHARE_RECIPIENT SR WHERE SR.PST_ID = B.PST_ID), 0) AS SHARE_COUNT, " +
            "       EXISTS(SELECT 1 FROM USER_SAVED_POST USP WHERE USP.PST_ID = B.PST_ID AND USP.USER_ID = :userId) AS SAVED, " +
            "       EXISTS(SELECT 1 FROM BOARD_POST_REACTION BR WHERE BR.PST_ID = B.PST_ID AND BR.USER_ID = :userId AND BR.REACTION_CD = 'like') AS LIKED " +
            "FROM BOARD B " +
            "LEFT JOIN USERS U ON B.CRT_USER_ID = U.USER_ID " +
            "LEFT JOIN DEPARTMENT D ON U.DEPT_ID = D.DEPT_ID " +
            "LEFT JOIN JOB_GRADE J ON U.JBGD_CD = J.JBGD_CD " +
            "LEFT JOIN COMMUNITY C ON B.COMMUNITY_ID = C.COMMUNITY_ID " +
            "WHERE " + whereClause +
            " ORDER BY " + resolveOrderBy(sort) +
            " LIMIT :limit OFFSET :offset";

        List<BoardVO> items = namedJdbc.query(listSql, queryParams, this::mapBoardSummary);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("items", items);
        response.put("page", Math.max(page, 1));
        response.put("totalPages", totalPages);
        response.put("totalRecords", safeTotal);
        response.put("summary", buildSummary(userId, admin));
        response.put("pinnedItems", selectSidebarItems(userId, admin, "COALESCE(B.FIXED_YN, 'N') = 'Y'", "COALESCE(B.PUBLISHED_DT, B.FRST_CRT_DT) DESC", 5));
        response.put("closingPolls", selectSidebarItems(userId, admin,
            "COALESCE(B.PST_TYPE_CD, 'story') = 'poll' AND EXISTS (SELECT 1 FROM BOARD_POLL P WHERE P.PST_ID = B.PST_ID AND P.DEADLINE_DT IS NOT NULL AND P.DEADLINE_DT >= CURRENT_TIMESTAMP)",
            "(SELECT P.DEADLINE_DT FROM BOARD_POLL P WHERE P.PST_ID = B.PST_ID) ASC NULLS LAST",
            5));
        response.put("todoItems", selectSidebarItems(userId, admin,
            "COALESCE(B.PST_TYPE_CD, 'story') = 'todo' AND EXISTS (SELECT 1 FROM BOARD_TODO_ASSIGNEE TA WHERE TA.PST_ID = B.PST_ID AND TA.USER_ID = :userId AND COALESCE(TA.STATUS_CD, 'requested') <> 'done')",
            "(SELECT T.DUE_DT FROM BOARD_TODO T WHERE T.PST_ID = B.PST_ID) ASC NULLS LAST",
            5));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public BoardVO readBoardDetail(String pstId, String userId, boolean admin) {
        BoardVO board = boardMapper.selectBoard(pstId);
        if (board == null || "Y".equalsIgnoreCase(board.getDelYn())) {
            throw new EntityNotFoundException(Map.of("pstId", pstId));
        }
        if (!canRead(board, userId, admin)) {
            throw new AccessDeniedException("게시글을 조회할 수 없습니다.");
        }
        return hydrateBoard(board, userId, admin, true);
    }

    @Override
    @Transactional
    public BoardVO createBoard(BoardVO board, String userId, boolean admin) {
        prepareBoard(board, userId, admin, null);
        if (!boardService.createBoard(board)) {
            throw new IllegalStateException("게시글 등록에 실패했습니다.");
        }
        persistBoardRelations(board, userId);
        notifyNewBoard(board, userId);
        return readBoardDetail(board.getPstId(), userId, admin);
    }

    @Override
    @Transactional
    public BoardVO updateBoard(String pstId, BoardVO board, String userId, boolean admin) {
        BoardVO existing = boardMapper.selectBoard(pstId);
        if (existing == null) {
            throw new EntityNotFoundException(Map.of("pstId", pstId));
        }
        if (!canManage(existing, userId, admin)) {
            throw new AccessDeniedException("게시글을 수정할 수 없습니다.");
        }

        prepareBoard(board, userId, admin, existing);
        board.setPstId(pstId);
        board.setCrtUserId(existing.getCrtUserId());
        board.setLastChgUserId(userId);

        cleanupBoardRelations(pstId, false);
        if (!boardService.modifyBoard(board)) {
            throw new IllegalStateException("게시글 수정에 실패했습니다.");
        }
        persistBoardRelations(board, userId);
        return readBoardDetail(pstId, userId, admin);
    }

    @Override
    @Transactional
    public void deleteBoard(String pstId, String userId, boolean admin) {
        BoardVO existing = boardMapper.selectBoard(pstId);
        if (existing == null) {
            throw new EntityNotFoundException(Map.of("pstId", pstId));
        }
        if (!canManage(existing, userId, admin)) {
            throw new AccessDeniedException("게시글을 삭제할 수 없습니다.");
        }

        cleanupBoardRelations(pstId, true);
        if (!boardService.removeBoard(pstId)) {
            throw new IllegalStateException("게시글 삭제에 실패했습니다.");
        }
    }

    @Override
    @Transactional
    public Map<String, Object> toggleLike(String pstId, String userId, boolean admin) {
        BoardVO board = requireReadableBoard(pstId, userId, admin);
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("pstId", board.getPstId())
            .addValue("userId", userId);

        Integer exists = namedJdbc.queryForObject(
            "SELECT COUNT(*) FROM BOARD_POST_REACTION WHERE PST_ID = :pstId AND USER_ID = :userId AND REACTION_CD = 'like'",
            params,
            Integer.class
        );
        boolean liked;
        if (exists != null && exists > 0) {
            namedJdbc.update("DELETE FROM BOARD_POST_REACTION WHERE PST_ID = :pstId AND USER_ID = :userId AND REACTION_CD = 'like'", params);
            liked = false;
        } else {
            namedJdbc.update(
                "INSERT INTO BOARD_POST_REACTION (PST_ID, USER_ID, REACTION_CD, CRT_DT) VALUES (:pstId, :userId, 'like', CURRENT_TIMESTAMP)",
                params
            );
            liked = true;
        }

        Integer likeCount = namedJdbc.queryForObject(
            "SELECT COUNT(*) FROM BOARD_POST_REACTION WHERE PST_ID = :pstId AND REACTION_CD = 'like'",
            params,
            Integer.class
        );
        return Map.of("liked", liked, "likeCount", likeCount == null ? 0 : likeCount);
    }

    @Override
    @Transactional(readOnly = true)
    public List<?> readReaders(String pstId, String userId, boolean admin) {
        requireReadableBoard(pstId, userId, admin);
        return namedJdbc.query(
            "SELECT R.USER_ID, R.READ_DT, U.USER_NM, D.DEPT_NM, J.JBGD_NM " +
                "FROM USER_FEED_READ R " +
                "JOIN USERS U ON R.USER_ID = U.USER_ID " +
                "LEFT JOIN DEPARTMENT D ON U.DEPT_ID = D.DEPT_ID " +
                "LEFT JOIN JOB_GRADE J ON U.JBGD_CD = J.JBGD_CD " +
                "WHERE R.PST_ID = :pstId ORDER BY R.READ_DT DESC",
            Map.of("pstId", pstId),
            (rs, rowNum) -> Map.of(
                "userId", rs.getString("USER_ID"),
                "userNm", rs.getString("USER_NM"),
                "deptNm", rs.getString("DEPT_NM"),
                "jbgdNm", rs.getString("JBGD_NM"),
                "readDt", rs.getTimestamp("READ_DT") == null ? null : rs.getTimestamp("READ_DT").toLocalDateTime()
            )
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<?> readLikeUsers(String pstId, String userId, boolean admin) {
        requireReadableBoard(pstId, userId, admin);
        return readLikeUsersInternal(pstId);
    }

    @Override
    @Transactional
    public Map<String, Object> shareBoard(String pstId, BoardShareRequest request, String userId, boolean admin) {
        BoardVO board = requireReadableBoard(pstId, userId, admin);
        if ("private".equalsIgnoreCase(board.getVisibilityCd())) {
            throw new IllegalArgumentException("비공개 게시글은 공유할 수 없습니다.");
        }

        Set<String> recipients = new LinkedHashSet<>();
        List<Long> shareIds = new ArrayList<>();

        for (String targetUserId : safeList(request == null ? null : request.getUserIds())) {
            if (!StringUtils.hasText(targetUserId) || Objects.equals(targetUserId, userId)) {
                continue;
            }
            Long shareId = namedJdbc.queryForObject(
                "INSERT INTO BOARD_SHARE (PST_ID, SHARE_TYPE_CD, SHARE_TARGET_ID, SHARE_USER_ID, CRT_DT) " +
                    "VALUES (:pstId, 'user', :targetId, :userId, CURRENT_TIMESTAMP) RETURNING SHARE_ID",
                new MapSqlParameterSource()
                    .addValue("pstId", pstId)
                    .addValue("targetId", targetUserId)
                    .addValue("userId", userId),
                Long.class
            );
            shareIds.add(shareId);
            recipients.add(targetUserId);
        }

        for (Long communityId : safeList(request == null ? null : request.getCommunityIds())) {
            Long shareId = namedJdbc.queryForObject(
                "INSERT INTO BOARD_SHARE (PST_ID, SHARE_TYPE_CD, SHARE_TARGET_ID, SHARE_USER_ID, CRT_DT) " +
                    "VALUES (:pstId, 'community', :targetId, :userId, CURRENT_TIMESTAMP) RETURNING SHARE_ID",
                new MapSqlParameterSource()
                    .addValue("pstId", pstId)
                    .addValue("targetId", String.valueOf(communityId))
                    .addValue("userId", userId),
                Long.class
            );
            shareIds.add(shareId);
            recipients.addAll(namedJdbc.queryForList(
                "SELECT USER_ID FROM COMMUNITY_MEMBER WHERE COMMUNITY_ID = :communityId AND STATUS_CD = 'active'",
                Map.of("communityId", communityId),
                String.class
            ));
        }

        recipients.remove(userId);
        for (Long shareId : shareIds) {
            for (String recipient : recipients) {
                if (!StringUtils.hasText(recipient)) {
                    continue;
                }
                namedJdbc.update(
                    "INSERT INTO BOARD_SHARE_RECIPIENT (SHARE_ID, PST_ID, RECIPIENT_USER_ID, CRT_DT) " +
                        "VALUES (:shareId, :pstId, :recipientUserId, CURRENT_TIMESTAMP) " +
                        "ON CONFLICT (PST_ID, RECIPIENT_USER_ID) DO UPDATE SET CRT_DT = EXCLUDED.CRT_DT",
                    new MapSqlParameterSource()
                        .addValue("shareId", shareId)
                        .addValue("pstId", pstId)
                        .addValue("recipientUserId", recipient)
                );
            }
        }

        return Map.of(
            "success", true,
            "recipientCount", recipients.size(),
            "board", readBoardDetail(pstId, userId, admin)
        );
    }

    @Override
    @Transactional
    public Map<String, Object> reportBoard(String pstId, BoardReportRequest request, String userId, boolean admin) {
        requireReadableBoard(pstId, userId, admin);
        if (request == null || !StringUtils.hasText(request.getReasonText())) {
            throw new IllegalArgumentException("신고 사유를 입력해 주세요.");
        }

        try {
            Long reportId = namedJdbc.queryForObject(
                "INSERT INTO BOARD_REPORT (PST_ID, REPORTER_ID, REASON_TEXT, CRT_DT, STATUS_CD) " +
                    "VALUES (:pstId, :reporterId, :reasonText, CURRENT_TIMESTAMP, 'submitted') RETURNING REPORT_ID",
                new MapSqlParameterSource()
                    .addValue("pstId", pstId)
                    .addValue("reporterId", userId)
                    .addValue("reasonText", request.getReasonText().trim()),
                Long.class
            );
            notifyAdmins("BOARD_REPORT", userId, pstId);
            return Map.of("success", true, "reportId", reportId);
        } catch (Exception ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("board_report")) {
                throw new IllegalArgumentException("이미 신고한 게시글입니다.");
            }
            throw ex;
        }
    }

    @Override
    @Transactional
    public BoardVO pinBoard(String pstId, BoardPinRequest request, String userId, boolean admin) {
        BoardVO board = requireReadableBoard(pstId, userId, admin);
        if (!canPin(board, userId, admin)) {
            throw new AccessDeniedException("상단 고정 권한이 없습니다.");
        }
        String fixedYn = request != null && "Y".equalsIgnoreCase(request.getFixedYn()) ? "Y" : "N";
        if ("Y".equals(fixedYn) && "private".equalsIgnoreCase(board.getVisibilityCd())) {
            throw new IllegalArgumentException("비공개 게시글은 고정할 수 없습니다.");
        }
        if ("Y".equals(fixedYn)) {
            validatePinLimit(board.getCommunityId(), pstId);
        }
        namedJdbc.update(
            "UPDATE BOARD SET FIXED_YN = :fixedYn, LAST_CHG_DT = CURRENT_TIMESTAMP, LAST_CHG_USER_ID = :userId WHERE PST_ID = :pstId",
            new MapSqlParameterSource().addValue("fixedYn", fixedYn).addValue("userId", userId).addValue("pstId", pstId)
        );
        return readBoardDetail(pstId, userId, admin);
    }

    @Override
    @Transactional
    public BoardVO votePoll(String pstId, BoardPollVoteRequest request, String userId, boolean admin) {
        BoardVO board = requireReadableBoard(pstId, userId, admin);
        if (!"poll".equalsIgnoreCase(board.getPstTypeCd())) {
            throw new IllegalArgumentException("설문 게시글이 아닙니다.");
        }
        BoardPollVO poll = loadPoll(pstId, userId);
        if (poll == null) {
            throw new IllegalArgumentException("설문 정보가 없습니다.");
        }
        if (poll.getDeadlineDt() != null && poll.getDeadlineDt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("이미 마감된 설문입니다.");
        }

        List<Long> optionIds = safeList(request == null ? null : request.getOptionIds()).stream()
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
        if (optionIds.isEmpty()) {
            throw new IllegalArgumentException("선택지를 선택해 주세요.");
        }
        if (!"Y".equalsIgnoreCase(poll.getMultipleYn()) && optionIds.size() > 1) {
            throw new IllegalArgumentException("단일 선택 설문입니다.");
        }

        List<Long> validOptionIds = namedJdbc.queryForList(
            "SELECT OPTION_ID FROM BOARD_POLL_OPTION WHERE PST_ID = :pstId",
            Map.of("pstId", pstId),
            Long.class
        );
        if (!validOptionIds.containsAll(optionIds)) {
            throw new IllegalArgumentException("유효하지 않은 선택지입니다.");
        }

        namedJdbc.update("DELETE FROM BOARD_POLL_RESPONSE WHERE PST_ID = :pstId AND USER_ID = :userId", Map.of("pstId", pstId, "userId", userId));
        for (Long optionId : optionIds) {
            namedJdbc.update(
                "INSERT INTO BOARD_POLL_RESPONSE (PST_ID, OPTION_ID, USER_ID, CRT_DT) VALUES (:pstId, :optionId, :userId, CURRENT_TIMESTAMP)",
                new MapSqlParameterSource().addValue("pstId", pstId).addValue("optionId", optionId).addValue("userId", userId)
            );
        }
        return readBoardDetail(pstId, userId, admin);
    }

    @Override
    @Transactional
    public BoardVO updateTodoStatus(String pstId, String assigneeUserId, BoardTodoStatusRequest request, String userId, boolean admin) {
        BoardVO board = requireReadableBoard(pstId, userId, admin);
        if (!"todo".equalsIgnoreCase(board.getPstTypeCd())) {
            throw new IllegalArgumentException("할일 게시글이 아닙니다.");
        }
        if (!admin && !Objects.equals(userId, assigneeUserId)) {
            throw new AccessDeniedException("담당자 본인만 상태를 변경할 수 있습니다.");
        }
        String nextStatus = normalizeTodoStatus(request == null ? null : request.getStatusCd());
        int updated = namedJdbc.update(
            "UPDATE BOARD_TODO_ASSIGNEE SET STATUS_CD = :statusCd, LAST_CHG_DT = CURRENT_TIMESTAMP WHERE PST_ID = :pstId AND USER_ID = :assigneeUserId",
            new MapSqlParameterSource()
                .addValue("statusCd", nextStatus)
                .addValue("pstId", pstId)
                .addValue("assigneeUserId", assigneeUserId)
        );
        if (updated == 0) {
            throw new EntityNotFoundException(Map.of("pstId", pstId, "userId", assigneeUserId));
        }

        List<String> statuses = namedJdbc.queryForList("SELECT STATUS_CD FROM BOARD_TODO_ASSIGNEE WHERE PST_ID = :pstId", Map.of("pstId", pstId), String.class);
        String summary = "requested";
        if (!statuses.isEmpty() && statuses.stream().allMatch("done"::equalsIgnoreCase)) {
            summary = "done";
        } else if (statuses.stream().anyMatch(status -> "in_progress".equalsIgnoreCase(status) || "done".equalsIgnoreCase(status))) {
            summary = "in_progress";
        }
        namedJdbc.update("UPDATE BOARD_TODO SET STATUS_CD = :statusCd WHERE PST_ID = :pstId", Map.of("statusCd", summary, "pstId", pstId));
        return readBoardDetail(pstId, userId, admin);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> readScheduleAvailability(BoardAvailabilityRequest request, String userId) {
        LocalDateTime startDt = request == null ? null : request.getStartDt();
        LocalDateTime endDt = request == null ? null : request.getEndDt();
        List<String> userIds = request == null ? List.of() : safeList(request.getUserIds());
        if (startDt == null || endDt == null || userIds.isEmpty()) {
            return Map.of("items", List.of());
        }

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("startDt", Timestamp.valueOf(startDt))
            .addValue("endDt", Timestamp.valueOf(endDt))
            .addValue("userIds", userIds);

        List<Map<String, Object>> items = namedJdbc.query(
            "SELECT U.USER_ID, U.USER_NM, D.DEPT_NM, " +
                "       EXISTS (SELECT 1 FROM USER_SCHEDULE US WHERE US.USER_ID = U.USER_ID AND COALESCE(US.DEL_YN, 'N') = 'N' AND US.SCHD_STRT_DT < :endDt AND US.SCHD_END_DT > :startDt) AS USER_BUSY, " +
                "       EXISTS (SELECT 1 FROM DEPARTMENT_SCHEDULE DS WHERE DS.DEPT_ID = U.DEPT_ID AND COALESCE(DS.DEL_YN, 'N') = 'N' AND DS.SCHD_STRT_DT < :endDt AND DS.SCHD_END_DT > :startDt) AS DEPT_BUSY, " +
                "       EXISTS (SELECT 1 FROM BOARD_SCHEDULE_ATTENDEE BSA JOIN BOARD_SCHEDULE BS ON BS.PST_ID = BSA.PST_ID JOIN BOARD B ON B.PST_ID = BSA.PST_ID WHERE BSA.USER_ID = U.USER_ID AND COALESCE(B.DEL_YN, 'N') = 'N' AND BS.START_DT < :endDt AND BS.END_DT > :startDt) AS BOARD_BUSY " +
                "FROM USERS U LEFT JOIN DEPARTMENT D ON U.DEPT_ID = D.DEPT_ID WHERE U.USER_ID IN (:userIds) ORDER BY U.USER_NM",
            params,
            (rs, rowNum) -> {
                boolean busy = rs.getBoolean("USER_BUSY") || rs.getBoolean("DEPT_BUSY") || rs.getBoolean("BOARD_BUSY");
                return Map.of(
                    "userId", rs.getString("USER_ID"),
                    "userNm", rs.getString("USER_NM"),
                    "deptNm", rs.getString("DEPT_NM"),
                    "busy", busy
                );
            }
        );
        return Map.of("items", items);
    }

    @Override
    @Transactional
    public int publishDueScheduledPosts() {
        List<String> dueIds = namedJdbc.queryForList(
            "SELECT PST_ID FROM BOARD WHERE COALESCE(DEL_YN, 'N') = 'N' AND COALESCE(PUBLISH_STATE_CD, 'published') = 'scheduled' AND RESERVED_PUBLISH_DT IS NOT NULL AND RESERVED_PUBLISH_DT <= CURRENT_TIMESTAMP",
            Map.of(),
            String.class
        );
        for (String pstId : dueIds) {
            namedJdbc.update(
                "UPDATE BOARD SET PUBLISH_STATE_CD = 'published', PUBLISHED_DT = CURRENT_TIMESTAMP WHERE PST_ID = :pstId",
                Map.of("pstId", pstId)
            );
            BoardVO board = boardMapper.selectBoard(pstId);
            if (board != null) {
                notifyNewBoard(board, board.getCrtUserId());
            }
        }
        return dueIds.size();
    }

    private String buildListWhereClause(Map<String, String> params, MapSqlParameterSource queryParams, String userId, boolean admin) {
        List<String> conditions = new ArrayList<>();
        conditions.add("COALESCE(B.DEL_YN, 'N') = 'N'");
        conditions.add(accessPredicate("B"));

        String scope = safeLower(params.get("scope"));
        switch (scope) {
            case "saved" -> conditions.add("EXISTS (SELECT 1 FROM USER_SAVED_POST USP WHERE USP.PST_ID = B.PST_ID AND USP.USER_ID = :userId)");
            case "mine" -> conditions.add("B.CRT_USER_ID = :userId");
            case "scheduled" -> conditions.add("COALESCE(B.PUBLISH_STATE_CD, 'published') = 'scheduled'");
            case "shared" -> conditions.add("EXISTS (SELECT 1 FROM BOARD_SHARE_RECIPIENT SR WHERE SR.PST_ID = B.PST_ID AND SR.RECIPIENT_USER_ID = :userId)");
            case "poll" -> conditions.add("COALESCE(B.PST_TYPE_CD, 'story') = 'poll'");
            case "schedule" -> conditions.add("COALESCE(B.PST_TYPE_CD, 'story') = 'schedule'");
            case "todo" -> conditions.add("COALESCE(B.PST_TYPE_CD, 'story') = 'todo'");
            case "pinned" -> conditions.add("COALESCE(B.FIXED_YN, 'N') = 'Y'");
            default -> {
            }
        }

        if (!admin && !"scheduled".equals(scope)) {
            conditions.add("(COALESCE(B.PUBLISH_STATE_CD, 'published') <> 'scheduled' OR B.RESERVED_PUBLISH_DT IS NULL OR B.RESERVED_PUBLISH_DT <= CURRENT_TIMESTAMP OR B.CRT_USER_ID = :userId)");
        }

        if (StringUtils.hasText(params.get("type")) && !"all".equalsIgnoreCase(params.get("type"))) {
            queryParams.addValue("pstTypeCd", params.get("type"));
            conditions.add("COALESCE(B.PST_TYPE_CD, 'story') = :pstTypeCd");
        }

        if (StringUtils.hasText(params.get("communityId")) && !"all".equalsIgnoreCase(params.get("communityId"))) {
            queryParams.addValue("communityId", Long.valueOf(params.get("communityId")));
            conditions.add("B.COMMUNITY_ID = :communityId");
        }

        if (StringUtils.hasText(params.get("importance")) && !"all".equalsIgnoreCase(params.get("importance"))) {
            queryParams.addValue("importanceCd", params.get("importance"));
            conditions.add("COALESCE(B.IMPORTANCE_CD, 'normal') = :importanceCd");
        }

        if (StringUtils.hasText(params.get("dateFrom"))) {
            queryParams.addValue("dateFrom", Timestamp.valueOf(params.get("dateFrom") + " 00:00:00"));
            conditions.add("COALESCE(B.PUBLISHED_DT, B.FRST_CRT_DT) >= :dateFrom");
        }

        if (StringUtils.hasText(params.get("dateTo"))) {
            queryParams.addValue("dateTo", Timestamp.valueOf(params.get("dateTo") + " 23:59:59"));
            conditions.add("COALESCE(B.PUBLISHED_DT, B.FRST_CRT_DT) <= :dateTo");
        }

        if (StringUtils.hasText(params.get("q"))) {
            queryParams.addValue("q", "%" + params.get("q").trim() + "%");
            switch (safeLower(params.get("searchType"))) {
                case "title" -> conditions.add("B.PST_TTL ILIKE :q");
                case "content" -> conditions.add("B.CONTENTS ILIKE :q");
                case "writer" -> conditions.add("EXISTS (SELECT 1 FROM USERS UQ WHERE UQ.USER_ID = B.CRT_USER_ID AND UQ.USER_NM ILIKE :q)");
                case "comment" -> conditions.add("EXISTS (SELECT 1 FROM BOARD_COMMENT BC WHERE BC.PST_ID = B.PST_ID AND COALESCE(BC.DEL_YN, 'N') = 'N' AND BC.CONTENTS ILIKE :q)");
                default -> conditions.add("(B.PST_TTL ILIKE :q OR B.CONTENTS ILIKE :q OR EXISTS (SELECT 1 FROM USERS UQ WHERE UQ.USER_ID = B.CRT_USER_ID AND UQ.USER_NM ILIKE :q))");
            }
        }
        return String.join(" AND ", conditions);
    }

    private String resolveOrderBy(String sort) {
        return switch (safeLower(sort)) {
            case "popular" -> "B.VIEW_CNT DESC, COALESCE(B.PUBLISHED_DT, B.FRST_CRT_DT) DESC";
            case "liked" -> "LIKE_COUNT DESC, COALESCE(B.PUBLISHED_DT, B.FRST_CRT_DT) DESC";
            case "deadline" -> "COALESCE((SELECT P.DEADLINE_DT FROM BOARD_POLL P WHERE P.PST_ID = B.PST_ID), (SELECT T.DUE_DT FROM BOARD_TODO T WHERE T.PST_ID = B.PST_ID), (SELECT S.START_DT FROM BOARD_SCHEDULE S WHERE S.PST_ID = B.PST_ID), COALESCE(B.PUBLISHED_DT, B.FRST_CRT_DT)) ASC";
            default -> "COALESCE(B.FIXED_YN, 'N') DESC, COALESCE(B.PUBLISHED_DT, B.FRST_CRT_DT) DESC, B.PST_ID DESC";
        };
    }

    private Map<String, Object> buildSummary(String userId, boolean admin) {
        return Map.of(
            "savedCount", countByCondition(userId, admin, "EXISTS (SELECT 1 FROM USER_SAVED_POST USP WHERE USP.PST_ID = B.PST_ID AND USP.USER_ID = :userId)"),
            "scheduledCount", countByCondition(userId, admin, "COALESCE(B.PUBLISH_STATE_CD, 'published') = 'scheduled'"),
            "pinnedCount", countByCondition(userId, admin, "COALESCE(B.FIXED_YN, 'N') = 'Y'"),
            "urgentCount", countByCondition(userId, admin, "COALESCE(B.IMPORTANCE_CD, 'normal') = 'urgent'")
        );
    }

    private int countByCondition(String userId, boolean admin, String extraCondition) {
        Integer count = namedJdbc.queryForObject(
            "SELECT COUNT(*) FROM BOARD B WHERE COALESCE(B.DEL_YN, 'N') = 'N' AND " + accessPredicate("B") + " AND " + extraCondition,
            new MapSqlParameterSource().addValue("userId", userId).addValue("admin", admin),
            Integer.class
        );
        return count == null ? 0 : count;
    }

    private List<BoardVO> selectSidebarItems(String userId, boolean admin, String extraCondition, String orderBy, int limit) {
        return namedJdbc.query(
            "SELECT B.PST_ID, B.BBS_CTGR_CD, B.COMMUNITY_ID, B.PST_TTL, B.CONTENTS, B.CRT_USER_ID, B.FIXED_YN, " +
                "B.FRST_CRT_DT, B.PST_FILE_ID, B.VIEW_CNT, B.IMPORTANCE_CD, B.LINK_URL, B.PST_TYPE_CD, B.PUBLISHED_DT, B.PUBLISH_STATE_CD, B.RESERVED_PUBLISH_DT, B.VISIBILITY_CD, B.COMMUNITY_AUTHOR_ROLE_CD, " +
                "U.USER_NM, D.DEPT_NM, J.JBGD_NM, C.COMMUNITY_NM, C.COMMUNITY_TYPE_CD, " +
                "COALESCE((SELECT COUNT(*) FROM BOARD_COMMENT BC WHERE BC.PST_ID = B.PST_ID AND COALESCE(BC.DEL_YN, 'N') = 'N'), 0) AS COMMENT_COUNT, " +
                "COALESCE((SELECT COUNT(*) FROM BOARD_POST_REACTION BR WHERE BR.PST_ID = B.PST_ID AND BR.REACTION_CD = 'like'), 0) AS LIKE_COUNT, " +
                "COALESCE((SELECT COUNT(*) FROM USER_FEED_READ R WHERE R.PST_ID = B.PST_ID), 0) AS READER_COUNT, " +
                "COALESCE((SELECT COUNT(*) FROM BOARD_SHARE_RECIPIENT SR WHERE SR.PST_ID = B.PST_ID), 0) AS SHARE_COUNT, " +
                "EXISTS(SELECT 1 FROM USER_SAVED_POST USP WHERE USP.PST_ID = B.PST_ID AND USP.USER_ID = :userId) AS SAVED, " +
                "EXISTS(SELECT 1 FROM BOARD_POST_REACTION BR WHERE BR.PST_ID = B.PST_ID AND BR.USER_ID = :userId AND BR.REACTION_CD = 'like') AS LIKED " +
                "FROM BOARD B " +
                "LEFT JOIN USERS U ON B.CRT_USER_ID = U.USER_ID " +
                "LEFT JOIN DEPARTMENT D ON U.DEPT_ID = D.DEPT_ID " +
                "LEFT JOIN JOB_GRADE J ON U.JBGD_CD = J.JBGD_CD " +
                "LEFT JOIN COMMUNITY C ON B.COMMUNITY_ID = C.COMMUNITY_ID " +
                "WHERE COALESCE(B.DEL_YN, 'N') = 'N' AND " + accessPredicate("B") + " AND " + extraCondition +
                " ORDER BY " + orderBy + " LIMIT :limit",
            new MapSqlParameterSource().addValue("userId", userId).addValue("admin", admin).addValue("limit", limit),
            this::mapBoardSummary
        );
    }

    private String accessPredicate(String alias) {
        return "(" +
            ":admin = TRUE " +
            "OR " + alias + ".CRT_USER_ID = :userId " +
            "OR EXISTS (SELECT 1 FROM BOARD_SHARE_RECIPIENT SR WHERE SR.PST_ID = " + alias + ".PST_ID AND SR.RECIPIENT_USER_ID = :userId) " +
            "OR (COALESCE(" + alias + ".VISIBILITY_CD, 'community') <> 'private' AND (" +
            alias + ".BBS_CTGR_CD = 'F101' OR " + alias + ".COMMUNITY_ID IS NULL OR " +
            "EXISTS (SELECT 1 FROM COMMUNITY_MEMBER CM WHERE CM.COMMUNITY_ID = " + alias + ".COMMUNITY_ID AND CM.USER_ID = :userId AND CM.STATUS_CD = 'active' " +
            "AND (" +
                "COALESCE((SELECT C.COMMUNITY_TYPE_CD FROM COMMUNITY C WHERE C.COMMUNITY_ID = " + alias + ".COMMUNITY_ID), 'general') <> 'notice' " +
                "OR CM.ROLE_CD IN ('owner', 'operator') " +
                "OR COALESCE(" + alias + ".COMMUNITY_AUTHOR_ROLE_CD, 'member') IN ('owner', 'operator')" +
            ")))" +
            "))";
    }

    private BoardVO mapBoardSummary(ResultSet rs, int rowNum) throws SQLException {
        BoardVO board = new BoardVO();
        board.setPstId(rs.getString("PST_ID"));
        board.setBbsCtgrCd(rs.getString("BBS_CTGR_CD"));
        board.setCommunityId(getLong(rs, "COMMUNITY_ID"));
        board.setPstTtl(rs.getString("PST_TTL"));
        board.setContents(rs.getString("CONTENTS"));
        board.setCrtUserId(rs.getString("CRT_USER_ID"));
        board.setFixedYn(rs.getString("FIXED_YN"));
        board.setFrstCrtDt(toLocalDateTime(rs.getTimestamp("FRST_CRT_DT")));
        board.setPstFileId(rs.getString("PST_FILE_ID"));
        board.setViewCnt(getInt(rs, "VIEW_CNT"));
        board.setImportanceCd(rs.getString("IMPORTANCE_CD"));
        board.setLinkUrl(rs.getString("LINK_URL"));
        board.setPstTypeCd(rs.getString("PST_TYPE_CD"));
        board.setPublishedDt(toLocalDateTime(rs.getTimestamp("PUBLISHED_DT")));
        board.setPublishStateCd(rs.getString("PUBLISH_STATE_CD"));
        board.setReservedPublishDt(toLocalDateTime(rs.getTimestamp("RESERVED_PUBLISH_DT")));
        board.setVisibilityCd(rs.getString("VISIBILITY_CD"));
        board.setCommunityAuthorRoleCd(rs.getString("COMMUNITY_AUTHOR_ROLE_CD"));
        board.setUserNm(rs.getString("USER_NM"));
        board.setDeptNm(rs.getString("DEPT_NM"));
        board.setJbgdNm(rs.getString("JBGD_NM"));
        board.setCommunityNm(rs.getString("COMMUNITY_NM"));
        board.setCommunityTypeCd(rs.getString("COMMUNITY_TYPE_CD"));
        board.setCommentCount(getInt(rs, "COMMENT_COUNT"));
        board.setLikeCount(getInt(rs, "LIKE_COUNT"));
        board.setReaderCount(getInt(rs, "READER_COUNT"));
        board.setShareCount(getInt(rs, "SHARE_COUNT"));
        board.setSaved(rs.getBoolean("SAVED"));
        board.setLiked(rs.getBoolean("LIKED"));
        return board;
    }

    private BoardVO hydrateBoard(BoardVO board, String userId, boolean admin, boolean includeCollections) {
        attachCounts(board, userId);
        board.setManageable(canManage(board, userId, admin));
        board.setPinnable(canPin(board, userId, admin));
        board.setShareable(!"private".equalsIgnoreCase(board.getVisibilityCd()));

        if (StringUtils.hasText(board.getPstFileId())) {
            board.setAttachments(fileDetailService.readFileDetailList(board.getPstFileId()));
        } else {
            board.setAttachments(List.of());
        }

        if ("poll".equalsIgnoreCase(board.getPstTypeCd())) {
            board.setPoll(loadPoll(board.getPstId(), userId));
            board.setPollOptions(board.getPoll() == null ? List.of() : board.getPoll().getOptions());
        }
        if ("schedule".equalsIgnoreCase(board.getPstTypeCd())) {
            board.setSchedule(loadSchedule(board.getPstId()));
        }
        if ("todo".equalsIgnoreCase(board.getPstTypeCd())) {
            board.setTodo(loadTodo(board.getPstId()));
        }
        if (includeCollections) {
            board.setComments(loadComments(board.getPstId()));
            board.setLikes(readLikeUsersInternal(board.getPstId()));
            board.setShareRecipients(loadShareRecipients(board.getPstId()));
            board.setMentions(loadPostMentions(board.getPstId()));
        }
        return board;
    }

    private void attachCounts(BoardVO board, String userId) {
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("pstId", board.getPstId()).addValue("userId", userId);
        board.setCommentCount(namedJdbc.queryForObject("SELECT COUNT(*) FROM BOARD_COMMENT WHERE PST_ID = :pstId AND COALESCE(DEL_YN, 'N') = 'N'", params, Integer.class));
        board.setLikeCount(namedJdbc.queryForObject("SELECT COUNT(*) FROM BOARD_POST_REACTION WHERE PST_ID = :pstId AND REACTION_CD = 'like'", params, Integer.class));
        board.setReaderCount(namedJdbc.queryForObject("SELECT COUNT(*) FROM USER_FEED_READ WHERE PST_ID = :pstId", params, Integer.class));
        board.setShareCount(namedJdbc.queryForObject("SELECT COUNT(*) FROM BOARD_SHARE_RECIPIENT WHERE PST_ID = :pstId", params, Integer.class));
        board.setSaved(namedJdbc.queryForObject("SELECT COUNT(*) > 0 FROM USER_SAVED_POST WHERE PST_ID = :pstId AND USER_ID = :userId", params, Boolean.class));
        board.setLiked(namedJdbc.queryForObject("SELECT COUNT(*) > 0 FROM BOARD_POST_REACTION WHERE PST_ID = :pstId AND USER_ID = :userId AND REACTION_CD = 'like'", params, Boolean.class));
    }

    private List<BoardCommentVO> loadComments(String pstId) {
        List<BoardCommentVO> comments = boardCommentMapper.selectBoardCommentList(pstId);
        for (BoardCommentVO comment : comments) {
            if (StringUtils.hasText(comment.getCmntFileId())) {
                comment.setAttachments(fileDetailService.readFileDetailList(comment.getCmntFileId()));
            } else {
                comment.setAttachments(List.of());
            }
        }
        return comments;
    }

    private BoardPollVO loadPoll(String pstId, String userId) {
        try {
            BoardPollVO poll = namedJdbc.queryForObject(
                "SELECT PST_ID, MULTIPLE_YN, ANONYMOUS_YN, RESULT_OPEN_YN, PARTICIPANT_OPEN_YN, DEADLINE_DT FROM BOARD_POLL WHERE PST_ID = :pstId",
                Map.of("pstId", pstId),
                (rs, rowNum) -> {
                    BoardPollVO item = new BoardPollVO();
                    item.setPstId(rs.getString("PST_ID"));
                    item.setMultipleYn(rs.getString("MULTIPLE_YN"));
                    item.setAnonymousYn(rs.getString("ANONYMOUS_YN"));
                    item.setResultOpenYn(rs.getString("RESULT_OPEN_YN"));
                    item.setParticipantOpenYn(rs.getString("PARTICIPANT_OPEN_YN"));
                    item.setDeadlineDt(toLocalDateTime(rs.getTimestamp("DEADLINE_DT")));
                    return item;
                }
            );
            List<Long> mySelections = namedJdbc.queryForList(
                "SELECT OPTION_ID FROM BOARD_POLL_RESPONSE WHERE PST_ID = :pstId AND USER_ID = :userId",
                new MapSqlParameterSource().addValue("pstId", pstId).addValue("userId", userId),
                Long.class
            );
            List<BoardPollOptionVO> options = namedJdbc.query(
                "SELECT O.OPTION_ID, O.PST_ID, O.OPTION_TEXT, O.SORT_ORDR, COALESCE((SELECT COUNT(*) FROM BOARD_POLL_RESPONSE R WHERE R.OPTION_ID = O.OPTION_ID), 0) AS VOTE_COUNT " +
                    "FROM BOARD_POLL_OPTION O WHERE O.PST_ID = :pstId ORDER BY O.SORT_ORDR, O.OPTION_ID",
                Map.of("pstId", pstId),
                (rs, rowNum) -> {
                    BoardPollOptionVO option = new BoardPollOptionVO();
                    option.setOptionId(rs.getLong("OPTION_ID"));
                    option.setPstId(rs.getString("PST_ID"));
                    option.setOptionText(rs.getString("OPTION_TEXT"));
                    option.setSortOrdr(getInt(rs, "SORT_ORDR"));
                    option.setVoteCount(getInt(rs, "VOTE_COUNT"));
                    option.setSelected(mySelections.contains(option.getOptionId()));
                    return option;
                }
            );
            poll.setOptions(options);
            poll.setTotalResponses(options.stream().map(BoardPollOptionVO::getVoteCount).filter(Objects::nonNull).reduce(0, Integer::sum));
            poll.setVoted(!mySelections.isEmpty());
            poll.setClosed(poll.getDeadlineDt() != null && poll.getDeadlineDt().isBefore(LocalDateTime.now()));
            return poll;
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private BoardScheduleVO loadSchedule(String pstId) {
        try {
            BoardScheduleVO schedule = namedJdbc.queryForObject(
                "SELECT PST_ID, START_DT, END_DT, ALLDAY_YN, REPEAT_RULE, PLACE_TEXT, PLACE_URL, REMINDER_MINUTES, VIDEO_MEETING_YN, MEETING_ROOM_ID, MEETING_RESERVATION_ID FROM BOARD_SCHEDULE WHERE PST_ID = :pstId",
                Map.of("pstId", pstId),
                (rs, rowNum) -> {
                    BoardScheduleVO item = new BoardScheduleVO();
                    item.setPstId(rs.getString("PST_ID"));
                    item.setStartDt(toLocalDateTime(rs.getTimestamp("START_DT")));
                    item.setEndDt(toLocalDateTime(rs.getTimestamp("END_DT")));
                    item.setAlldayYn(rs.getString("ALLDAY_YN"));
                    item.setRepeatRule(rs.getString("REPEAT_RULE"));
                    item.setPlaceText(rs.getString("PLACE_TEXT"));
                    item.setPlaceUrl(rs.getString("PLACE_URL"));
                    item.setReminderMinutes(getInt(rs, "REMINDER_MINUTES"));
                    item.setVideoMeetingYn(rs.getString("VIDEO_MEETING_YN"));
                    item.setMeetingRoomId(rs.getString("MEETING_ROOM_ID"));
                    item.setMeetingReservationId(getInt(rs, "MEETING_RESERVATION_ID"));
                    return item;
                }
            );
            schedule.setAttendees(namedJdbc.query(
                "SELECT A.PST_ID, A.USER_ID, A.ATTENDANCE_STTS_CD, U.USER_NM, D.DEPT_NM, F.FILE_PATH " +
                    "FROM BOARD_SCHEDULE_ATTENDEE A JOIN USERS U ON A.USER_ID = U.USER_ID " +
                    "LEFT JOIN DEPARTMENT D ON U.DEPT_ID = D.DEPT_ID " +
                    "LEFT JOIN FILE_DETAIL F ON U.USER_IMG_FILE_ID = F.FILE_ID " +
                    "WHERE A.PST_ID = :pstId ORDER BY U.USER_NM",
                Map.of("pstId", pstId),
                (rs, rowNum) -> {
                    BoardScheduleAttendeeVO attendee = new BoardScheduleAttendeeVO();
                    attendee.setPstId(rs.getString("PST_ID"));
                    attendee.setUserId(rs.getString("USER_ID"));
                    attendee.setAttendanceSttsCd(rs.getString("ATTENDANCE_STTS_CD"));
                    attendee.setUserNm(rs.getString("USER_NM"));
                    attendee.setDeptNm(rs.getString("DEPT_NM"));
                    attendee.setFilePath(rs.getString("FILE_PATH"));
                    return attendee;
                }
            ));
            return schedule;
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private BoardTodoVO loadTodo(String pstId) {
        try {
            BoardTodoVO todo = namedJdbc.queryForObject(
                "SELECT PST_ID, DUE_DT, STATUS_CD FROM BOARD_TODO WHERE PST_ID = :pstId",
                Map.of("pstId", pstId),
                (rs, rowNum) -> {
                    BoardTodoVO item = new BoardTodoVO();
                    item.setPstId(rs.getString("PST_ID"));
                    item.setDueDt(toLocalDateTime(rs.getTimestamp("DUE_DT")));
                    item.setStatusCd(rs.getString("STATUS_CD"));
                    return item;
                }
            );
            List<BoardTodoAssigneeVO> assignees = namedJdbc.query(
                "SELECT A.PST_ID, A.USER_ID, A.STATUS_CD, A.CRT_DT, A.LAST_CHG_DT, U.USER_NM, D.DEPT_NM, J.JBGD_NM, F.FILE_PATH " +
                    "FROM BOARD_TODO_ASSIGNEE A JOIN USERS U ON A.USER_ID = U.USER_ID " +
                    "LEFT JOIN DEPARTMENT D ON U.DEPT_ID = D.DEPT_ID " +
                    "LEFT JOIN JOB_GRADE J ON U.JBGD_CD = J.JBGD_CD " +
                    "LEFT JOIN FILE_DETAIL F ON U.USER_IMG_FILE_ID = F.FILE_ID " +
                    "WHERE A.PST_ID = :pstId ORDER BY U.USER_NM",
                Map.of("pstId", pstId),
                (rs, rowNum) -> {
                    BoardTodoAssigneeVO assignee = new BoardTodoAssigneeVO();
                    assignee.setPstId(rs.getString("PST_ID"));
                    assignee.setUserId(rs.getString("USER_ID"));
                    assignee.setStatusCd(rs.getString("STATUS_CD"));
                    assignee.setCrtDt(toLocalDateTime(rs.getTimestamp("CRT_DT")));
                    assignee.setLastChgDt(toLocalDateTime(rs.getTimestamp("LAST_CHG_DT")));
                    assignee.setUserNm(rs.getString("USER_NM"));
                    assignee.setDeptNm(rs.getString("DEPT_NM"));
                    assignee.setJbgdNm(rs.getString("JBGD_NM"));
                    assignee.setFilePath(rs.getString("FILE_PATH"));
                    return assignee;
                }
            );
            todo.setAssignees(assignees);
            todo.setTotalCount(assignees.size());
            todo.setDoneCount((int) assignees.stream().filter(assignee -> "done".equalsIgnoreCase(assignee.getStatusCd())).count());
            return todo;
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private List<BoardReactionVO> readLikeUsersInternal(String pstId) {
        return namedJdbc.query(
            "SELECT R.PST_ID, R.USER_ID, R.REACTION_CD, R.CRT_DT, U.USER_NM, D.DEPT_NM, F.FILE_PATH " +
                "FROM BOARD_POST_REACTION R JOIN USERS U ON R.USER_ID = U.USER_ID " +
                "LEFT JOIN DEPARTMENT D ON U.DEPT_ID = D.DEPT_ID " +
                "LEFT JOIN FILE_DETAIL F ON U.USER_IMG_FILE_ID = F.FILE_ID " +
                "WHERE R.PST_ID = :pstId AND R.REACTION_CD = 'like' ORDER BY R.CRT_DT DESC",
            Map.of("pstId", pstId),
            (rs, rowNum) -> {
                BoardReactionVO reaction = new BoardReactionVO();
                reaction.setPstId(rs.getString("PST_ID"));
                reaction.setUserId(rs.getString("USER_ID"));
                reaction.setReactionCd(rs.getString("REACTION_CD"));
                reaction.setCrtDt(toLocalDateTime(rs.getTimestamp("CRT_DT")));
                reaction.setUserNm(rs.getString("USER_NM"));
                reaction.setDeptNm(rs.getString("DEPT_NM"));
                reaction.setFilePath(rs.getString("FILE_PATH"));
                return reaction;
            }
        );
    }

    private List<BoardShareRecipientVO> loadShareRecipients(String pstId) {
        return namedJdbc.query(
            "SELECT SR.SHARE_ID, SR.PST_ID, SR.RECIPIENT_USER_ID, SR.CRT_DT, U.USER_NM, D.DEPT_NM, S.SHARE_TYPE_CD, S.SHARE_TARGET_ID " +
                "FROM BOARD_SHARE_RECIPIENT SR JOIN USERS U ON SR.RECIPIENT_USER_ID = U.USER_ID " +
                "LEFT JOIN DEPARTMENT D ON U.DEPT_ID = D.DEPT_ID " +
                "LEFT JOIN BOARD_SHARE S ON SR.SHARE_ID = S.SHARE_ID " +
                "WHERE SR.PST_ID = :pstId ORDER BY SR.CRT_DT DESC",
            Map.of("pstId", pstId),
            (rs, rowNum) -> {
                BoardShareRecipientVO item = new BoardShareRecipientVO();
                item.setShareId(rs.getLong("SHARE_ID"));
                item.setPstId(rs.getString("PST_ID"));
                item.setRecipientUserId(rs.getString("RECIPIENT_USER_ID"));
                item.setCrtDt(toLocalDateTime(rs.getTimestamp("CRT_DT")));
                item.setUserNm(rs.getString("USER_NM"));
                item.setDeptNm(rs.getString("DEPT_NM"));
                item.setShareTypeCd(rs.getString("SHARE_TYPE_CD"));
                item.setShareTargetId(rs.getString("SHARE_TARGET_ID"));
                return item;
            }
        );
    }

    private List<BoardMentionVO> loadPostMentions(String pstId) {
        return namedJdbc.query(
            "SELECT M.PST_ID, M.USER_ID, M.CRT_DT, U.USER_NM FROM BOARD_POST_MENTION M JOIN USERS U ON M.USER_ID = U.USER_ID WHERE M.PST_ID = :pstId ORDER BY M.CRT_DT DESC",
            Map.of("pstId", pstId),
            (rs, rowNum) -> {
                BoardMentionVO mention = new BoardMentionVO();
                mention.setPstId(rs.getString("PST_ID"));
                mention.setUserId(rs.getString("USER_ID"));
                mention.setCrtDt(toLocalDateTime(rs.getTimestamp("CRT_DT")));
                mention.setUserNm(rs.getString("USER_NM"));
                return mention;
            }
        );
    }

    private void prepareBoard(BoardVO board, String userId, boolean admin, BoardVO existing) {
        if (!StringUtils.hasText(board.getBbsCtgrCd()) || !StringUtils.hasText(board.getPstTtl()) || !StringUtils.hasText(board.getContents())) {
            throw new IllegalArgumentException("게시글 제목, 내용, 분류는 필수입니다.");
        }
        if ("F101".equals(board.getBbsCtgrCd()) && !admin) {
            throw new AccessDeniedException("공지사항은 관리자만 작성할 수 있습니다.");
        }
        if (board.getCommunityId() != null && !admin && !isCommunityMember(board.getCommunityId(), userId)) {
            throw new AccessDeniedException("해당 커뮤니티에 글을 작성할 권한이 없습니다.");
        }

        if (board.getCommunityId() != null) {
            if (existing != null
                && Objects.equals(existing.getCommunityId(), board.getCommunityId())
                && StringUtils.hasText(existing.getCommunityAuthorRoleCd())) {
                board.setCommunityAuthorRoleCd(existing.getCommunityAuthorRoleCd());
            } else {
                board.setCommunityAuthorRoleCd(resolveCommunityAuthorRole(board.getCommunityId(), existing == null ? userId : existing.getCrtUserId()));
            }
        } else {
            board.setCommunityAuthorRoleCd(null);
        }

        board.setFixedYn("Y".equalsIgnoreCase(board.getFixedYn()) ? "Y" : "N");
        board.setPstTypeCd(StringUtils.hasText(board.getPstTypeCd()) ? board.getPstTypeCd().toLowerCase(Locale.ROOT) : "story");
        board.setVisibilityCd(StringUtils.hasText(board.getVisibilityCd()) ? board.getVisibilityCd().toLowerCase(Locale.ROOT) : "community");
        board.setImportanceCd(normalizeImportance(board));

        if ("Y".equals(board.getFixedYn())) {
            if (!canPin(board, userId, admin)) {
                throw new AccessDeniedException("상단 고정 권한이 없습니다.");
            }
            if ("private".equalsIgnoreCase(board.getVisibilityCd())) {
                throw new IllegalArgumentException("비공개 게시글은 상단 고정할 수 없습니다.");
            }
            validatePinLimit(board.getCommunityId(), existing == null ? null : existing.getPstId());
        }

        LocalDateTime reserved = board.getReservedPublishDt();
        if (reserved != null && reserved.isAfter(LocalDateTime.now())) {
            board.setPublishStateCd("scheduled");
        } else {
            board.setPublishStateCd("published");
            board.setReservedPublishDt(null);
        }
    }

    private String defaultImportance(BoardVO board) {
        if ("F101".equals(board.getBbsCtgrCd()) || "Y".equalsIgnoreCase(board.getFixedYn())) {
            return "notice";
        }
        return "normal";
    }

    private String normalizeImportance(BoardVO board) {
        String normalized = safeLower(board == null ? null : board.getImportanceCd());
        if (!StringUtils.hasText(normalized)) {
            return defaultImportance(board);
        }

        Set<String> allowedCodes = "schedule".equalsIgnoreCase(board.getPstTypeCd()) ? SCHEDULE_IMPORTANCE_CODES : DEFAULT_IMPORTANCE_CODES;
        if (allowedCodes.contains(normalized)) {
            return normalized;
        }
        return defaultImportance(board);
    }

    private void persistBoardRelations(BoardVO board, String userId) {
        deleteSubtypeRows(board.getPstId());
        savePostMentions(board, userId);
        switch (safeLower(board.getPstTypeCd())) {
            case "poll" -> savePoll(board);
            case "schedule" -> saveSchedule(board, userId);
            case "todo" -> saveTodo(board);
            default -> {
            }
        }
    }

    private void savePoll(BoardVO board) {
        BoardPollVO poll = board.getPoll();
        List<BoardPollOptionVO> options = safeList(board.getPollOptions());
        if (poll == null || options.isEmpty()) {
            throw new IllegalArgumentException("설문 게시글은 최소 1개 이상의 선택지가 필요합니다.");
        }
        namedJdbc.update(
            "INSERT INTO BOARD_POLL (PST_ID, MULTIPLE_YN, ANONYMOUS_YN, RESULT_OPEN_YN, PARTICIPANT_OPEN_YN, DEADLINE_DT) VALUES (:pstId, :multipleYn, :anonymousYn, :resultOpenYn, :participantOpenYn, :deadlineDt)",
            new MapSqlParameterSource()
                .addValue("pstId", board.getPstId())
                .addValue("multipleYn", yesNo(poll.getMultipleYn()))
                .addValue("anonymousYn", yesNo(poll.getAnonymousYn()))
                .addValue("resultOpenYn", yesNo(poll.getResultOpenYn(), true))
                .addValue("participantOpenYn", yesNo(poll.getParticipantOpenYn(), true))
                .addValue("deadlineDt", poll.getDeadlineDt() == null ? null : Timestamp.valueOf(poll.getDeadlineDt()))
        );

        int order = 1;
        for (BoardPollOptionVO option : options) {
            if (!StringUtils.hasText(option.getOptionText())) {
                continue;
            }
            namedJdbc.update(
                "INSERT INTO BOARD_POLL_OPTION (PST_ID, OPTION_TEXT, SORT_ORDR) VALUES (:pstId, :optionText, :sortOrdr)",
                new MapSqlParameterSource().addValue("pstId", board.getPstId()).addValue("optionText", option.getOptionText().trim()).addValue("sortOrdr", order++)
            );
        }
    }

    private void saveSchedule(BoardVO board, String userId) {
        BoardScheduleVO schedule = board.getSchedule();
        if (schedule == null || schedule.getStartDt() == null || schedule.getEndDt() == null) {
            throw new IllegalArgumentException("일정 게시글은 시작/종료 시간이 필요합니다.");
        }
        Integer reservationId = createOrUpdateMeetingReservation(schedule, userId);
        namedJdbc.update(
            "INSERT INTO BOARD_SCHEDULE (PST_ID, START_DT, END_DT, ALLDAY_YN, REPEAT_RULE, PLACE_TEXT, PLACE_URL, REMINDER_MINUTES, VIDEO_MEETING_YN, MEETING_ROOM_ID, MEETING_RESERVATION_ID) " +
                "VALUES (:pstId, :startDt, :endDt, :alldayYn, :repeatRule, :placeText, :placeUrl, :reminderMinutes, :videoMeetingYn, :meetingRoomId, :meetingReservationId)",
            new MapSqlParameterSource()
                .addValue("pstId", board.getPstId())
                .addValue("startDt", Timestamp.valueOf(schedule.getStartDt()))
                .addValue("endDt", Timestamp.valueOf(schedule.getEndDt()))
                .addValue("alldayYn", yesNo(schedule.getAlldayYn()))
                .addValue("repeatRule", schedule.getRepeatRule())
                .addValue("placeText", schedule.getPlaceText())
                .addValue("placeUrl", schedule.getPlaceUrl())
                .addValue("reminderMinutes", schedule.getReminderMinutes())
                .addValue("videoMeetingYn", yesNo(schedule.getVideoMeetingYn()))
                .addValue("meetingRoomId", schedule.getMeetingRoomId())
                .addValue("meetingReservationId", reservationId)
        );

        for (BoardScheduleAttendeeVO attendee : safeList(schedule.getAttendees())) {
            if (!StringUtils.hasText(attendee.getUserId())) {
                continue;
            }
            namedJdbc.update(
                "INSERT INTO BOARD_SCHEDULE_ATTENDEE (PST_ID, USER_ID, ATTENDANCE_STTS_CD) VALUES (:pstId, :userId, :attendanceSttsCd)",
                new MapSqlParameterSource()
                    .addValue("pstId", board.getPstId())
                    .addValue("userId", attendee.getUserId())
                    .addValue("attendanceSttsCd", StringUtils.hasText(attendee.getAttendanceSttsCd()) ? attendee.getAttendanceSttsCd() : "invited")
            );
        }
    }

    private Integer createOrUpdateMeetingReservation(BoardScheduleVO schedule, String userId) {
        if (!"Y".equalsIgnoreCase(schedule.getVideoMeetingYn()) || !StringUtils.hasText(schedule.getMeetingRoomId())) {
            return null;
        }
        try {
            MeetingReservationVO reservation = new MeetingReservationVO();
            reservation.setReservationId(schedule.getMeetingReservationId());
            reservation.setRoomId(schedule.getMeetingRoomId());
            reservation.setUserId(userId);
            reservation.setTitle("게시판 일정 회의");
            reservation.setMeetingDate(schedule.getStartDt().toLocalDate());
            reservation.setStartTime(schedule.getStartDt().getHour());
            reservation.setEndTime(Math.max(schedule.getEndDt().getHour(), reservation.getStartTime() + 1));
            if (reservation.getReservationId() == null) {
                reservation = meetingReservationService.createMeetingReservation(reservation);
            } else {
                reservation = meetingReservationService.modifyMeetingReservation(reservation);
            }
            return reservation == null ? null : reservation.getReservationId();
        } catch (RuntimeException ex) {
            log.warn("Board schedule meeting reservation sync failed", ex);
            return null;
        }
    }

    private void saveTodo(BoardVO board) {
        BoardTodoVO todo = board.getTodo();
        if (todo == null || safeList(todo.getAssignees()).isEmpty()) {
            throw new IllegalArgumentException("할일 게시글은 최소 1명의 담당자가 필요합니다.");
        }
        namedJdbc.update(
            "INSERT INTO BOARD_TODO (PST_ID, DUE_DT, STATUS_CD) VALUES (:pstId, :dueDt, :statusCd)",
            new MapSqlParameterSource()
                .addValue("pstId", board.getPstId())
                .addValue("dueDt", todo.getDueDt() == null ? null : Timestamp.valueOf(todo.getDueDt()))
                .addValue("statusCd", normalizeTodoStatus(todo.getStatusCd()))
        );
        for (BoardTodoAssigneeVO assignee : safeList(todo.getAssignees())) {
            if (!StringUtils.hasText(assignee.getUserId())) {
                continue;
            }
            namedJdbc.update(
                "INSERT INTO BOARD_TODO_ASSIGNEE (PST_ID, USER_ID, STATUS_CD, CRT_DT, LAST_CHG_DT) VALUES (:pstId, :userId, :statusCd, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                new MapSqlParameterSource()
                    .addValue("pstId", board.getPstId())
                    .addValue("userId", assignee.getUserId())
                    .addValue("statusCd", normalizeTodoStatus(assignee.getStatusCd()))
            );
        }
    }

    private void savePostMentions(BoardVO board, String senderId) {
        namedJdbc.update("DELETE FROM BOARD_POST_MENTION WHERE PST_ID = :pstId", Map.of("pstId", board.getPstId()));
        Set<String> mentionUserIds = new LinkedHashSet<>(safeList(board.getMentionUserIds()));
        boolean mentionAll = Boolean.TRUE.equals(board.getMentionAll()) || String.valueOf(board.getContents()).contains("@전체");
        if (mentionAll && !"private".equalsIgnoreCase(board.getVisibilityCd())) {
            if (board.getCommunityId() != null) {
                mentionUserIds.addAll(namedJdbc.queryForList("SELECT USER_ID FROM COMMUNITY_MEMBER WHERE COMMUNITY_ID = :communityId AND STATUS_CD = 'active'", Map.of("communityId", board.getCommunityId()), String.class));
            } else {
                mentionUserIds.addAll(namedJdbc.queryForList("SELECT USER_ID FROM USERS WHERE COALESCE(RSGNTN_YN, 'N') = 'N'", Map.of(), String.class));
            }
        }
        mentionUserIds.remove(senderId);
        for (String targetUserId : mentionUserIds) {
            if (!StringUtils.hasText(targetUserId)) {
                continue;
            }
            namedJdbc.update(
                "INSERT INTO BOARD_POST_MENTION (PST_ID, USER_ID, CRT_DT) VALUES (:pstId, :userId, CURRENT_TIMESTAMP) ON CONFLICT (PST_ID, USER_ID) DO NOTHING",
                new MapSqlParameterSource().addValue("pstId", board.getPstId()).addValue("userId", targetUserId)
            );
            sendNotification(targetUserId, senderId, "BOARD_MENTION", board.getPstId());
        }
    }

    private void notifyNewBoard(BoardVO board, String senderId) {
        if (!"published".equalsIgnoreCase(board.getPublishStateCd()) || !"urgent".equalsIgnoreCase(board.getImportanceCd())) {
            return;
        }
        Set<String> receivers = new LinkedHashSet<>();
        if (board.getCommunityId() != null) {
            receivers.addAll(namedJdbc.queryForList("SELECT USER_ID FROM COMMUNITY_MEMBER WHERE COMMUNITY_ID = :communityId AND STATUS_CD = 'active'", Map.of("communityId", board.getCommunityId()), String.class));
        } else {
            receivers.addAll(namedJdbc.queryForList("SELECT USER_ID FROM USERS WHERE COALESCE(RSGNTN_YN, 'N') = 'N'", Map.of(), String.class));
        }
        receivers.remove(senderId);
        for (String receiver : receivers) {
            sendNotification(receiver, senderId, "BOARD_URGENT", board.getPstId());
        }
    }

    private void notifyAdmins(String alarmCode, String senderId, String pstId) {
        List<String> admins = namedJdbc.queryForList("SELECT USER_ID FROM USERS WHERE USER_ROLE ILIKE '%ADMIN%'", Map.of(), String.class);
        for (String receiverId : admins) {
            if (!Objects.equals(receiverId, senderId)) {
                sendNotification(receiverId, senderId, alarmCode, pstId);
            }
        }
    }

    private void sendNotification(String receiverId, String senderId, String alarmCode, String pk) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("receiverId", receiverId);
        payload.put("senderId", senderId);
        payload.put("alarmCode", alarmCode);
        payload.put("pk", pk);
        notificationService.sendNotification(payload);
    }

    private void cleanupBoardRelations(String pstId, boolean deleteReservation) {
        try {
            Integer reservationId = namedJdbc.queryForObject(
                "SELECT MEETING_RESERVATION_ID FROM BOARD_SCHEDULE WHERE PST_ID = :pstId",
                Map.of("pstId", pstId),
                Integer.class
            );
            if (reservationId != null) {
                meetingReservationService.removeMeetingReservation(reservationId);
            }
        } catch (EmptyResultDataAccessException ignored) {
        } catch (RuntimeException ex) {
            log.warn("Failed to remove linked meeting reservation for board {}", pstId, ex);
        }
        deleteSubtypeRows(pstId);
        namedJdbc.update("DELETE FROM BOARD_POST_MENTION WHERE PST_ID = :pstId", Map.of("pstId", pstId));
        if (deleteReservation) {
            namedJdbc.update("DELETE FROM BOARD_POST_REACTION WHERE PST_ID = :pstId", Map.of("pstId", pstId));
            namedJdbc.update("DELETE FROM BOARD_SHARE_RECIPIENT WHERE PST_ID = :pstId", Map.of("pstId", pstId));
            namedJdbc.update("DELETE FROM BOARD_SHARE WHERE PST_ID = :pstId", Map.of("pstId", pstId));
        }
    }

    private void deleteSubtypeRows(String pstId) {
        namedJdbc.update("DELETE FROM BOARD_POLL_RESPONSE WHERE PST_ID = :pstId", Map.of("pstId", pstId));
        namedJdbc.update("DELETE FROM BOARD_POLL_OPTION WHERE PST_ID = :pstId", Map.of("pstId", pstId));
        namedJdbc.update("DELETE FROM BOARD_POLL WHERE PST_ID = :pstId", Map.of("pstId", pstId));
        namedJdbc.update("DELETE FROM BOARD_SCHEDULE_ATTENDEE WHERE PST_ID = :pstId", Map.of("pstId", pstId));
        namedJdbc.update("DELETE FROM BOARD_SCHEDULE WHERE PST_ID = :pstId", Map.of("pstId", pstId));
        namedJdbc.update("DELETE FROM BOARD_TODO_ASSIGNEE WHERE PST_ID = :pstId", Map.of("pstId", pstId));
        namedJdbc.update("DELETE FROM BOARD_TODO WHERE PST_ID = :pstId", Map.of("pstId", pstId));
    }

    private BoardVO requireReadableBoard(String pstId, String userId, boolean admin) {
        BoardVO board = boardMapper.selectBoard(pstId);
        if (board == null) {
            throw new EntityNotFoundException(Map.of("pstId", pstId));
        }
        if (!canRead(board, userId, admin)) {
            throw new AccessDeniedException("게시글을 조회할 수 없습니다.");
        }
        return board;
    }

    private boolean canRead(BoardVO board, String userId, boolean admin) {
        if (board == null || "Y".equalsIgnoreCase(board.getDelYn())) {
            return false;
        }
        if (admin || Objects.equals(board.getCrtUserId(), userId)) {
            return true;
        }
        if ("scheduled".equalsIgnoreCase(board.getPublishStateCd())
            && board.getReservedPublishDt() != null
            && board.getReservedPublishDt().isAfter(LocalDateTime.now())) {
            return false;
        }
        boolean shared = exists(
            "SELECT COUNT(*) FROM BOARD_SHARE_RECIPIENT WHERE PST_ID = :pstId AND RECIPIENT_USER_ID = :userId",
            new MapSqlParameterSource().addValue("pstId", board.getPstId()).addValue("userId", userId)
        );
        if (shared) {
            return true;
        }
        if ("private".equalsIgnoreCase(board.getVisibilityCd())) {
            return false;
        }
        if ("F101".equals(board.getBbsCtgrCd()) || board.getCommunityId() == null) {
            return true;
        }
        if (!isCommunityMember(board.getCommunityId(), userId)) {
            return false;
        }
        if (!"notice".equalsIgnoreCase(board.getCommunityTypeCd())) {
            return true;
        }
        return isCommunityManager(board.getCommunityId(), userId)
            || "owner".equalsIgnoreCase(board.getCommunityAuthorRoleCd())
            || "operator".equalsIgnoreCase(board.getCommunityAuthorRoleCd());
    }

    private boolean canManage(BoardVO board, String userId, boolean admin) {
        if (board == null) {
            return false;
        }
        if (admin) {
            return true;
        }
        if ("F101".equals(board.getBbsCtgrCd())) {
            return false;
        }
        if (board.getCommunityId() != null && isCommunityManager(board.getCommunityId(), userId)) {
            return true;
        }
        return Objects.equals(board.getCrtUserId(), userId);
    }

    private boolean canPin(BoardVO board, String userId, boolean admin) {
        if (admin) {
            return true;
        }
        return board != null && board.getCommunityId() != null && isCommunityManager(board.getCommunityId(), userId);
    }

    private void validatePinLimit(Long communityId, String excludePstId) {
        String sql =
            "SELECT COUNT(*) FROM BOARD WHERE COALESCE(DEL_YN, 'N') = 'N' AND COALESCE(FIXED_YN, 'N') = 'Y' " +
                (communityId == null ? "AND COMMUNITY_ID IS NULL" : "AND COMMUNITY_ID = :communityId") +
                (StringUtils.hasText(excludePstId) ? " AND PST_ID <> :pstId" : "");
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (communityId != null) {
            params.addValue("communityId", communityId);
        }
        if (StringUtils.hasText(excludePstId)) {
            params.addValue("pstId", excludePstId);
        }
        Integer count = namedJdbc.queryForObject(sql, params, Integer.class);
        if (count != null && count >= PIN_LIMIT) {
            throw new IllegalArgumentException("상단 고정은 커뮤니티당 최대 10개까지 가능합니다.");
        }
    }

    private boolean isCommunityMember(Long communityId, String userId) {
        if (communityId == null || !StringUtils.hasText(userId)) {
            return false;
        }
        return exists(
            "SELECT COUNT(*) FROM COMMUNITY_MEMBER WHERE COMMUNITY_ID = :communityId AND USER_ID = :userId AND STATUS_CD = 'active'",
            new MapSqlParameterSource().addValue("communityId", communityId).addValue("userId", userId)
        );
    }

    private boolean isCommunityOwner(Long communityId, String userId) {
        if (communityId == null || !StringUtils.hasText(userId)) {
            return false;
        }
        return exists(
            "SELECT COUNT(*) FROM COMMUNITY_MEMBER WHERE COMMUNITY_ID = :communityId AND USER_ID = :userId AND ROLE_CD = 'owner' AND STATUS_CD = 'active'",
            new MapSqlParameterSource().addValue("communityId", communityId).addValue("userId", userId)
        );
    }

    private boolean isCommunityManager(Long communityId, String userId) {
        if (communityId == null || !StringUtils.hasText(userId)) {
            return false;
        }
        return exists(
            "SELECT COUNT(*) FROM COMMUNITY_MEMBER WHERE COMMUNITY_ID = :communityId AND USER_ID = :userId AND ROLE_CD IN ('owner', 'operator') AND STATUS_CD = 'active'",
            new MapSqlParameterSource().addValue("communityId", communityId).addValue("userId", userId)
        );
    }

    private String resolveCommunityAuthorRole(Long communityId, String authorUserId) {
        if (communityId == null || !StringUtils.hasText(authorUserId)) {
            return null;
        }
        try {
            String roleCd = namedJdbc.queryForObject(
                "SELECT ROLE_CD FROM COMMUNITY_MEMBER WHERE COMMUNITY_ID = :communityId AND USER_ID = :userId AND STATUS_CD = 'active'",
                new MapSqlParameterSource().addValue("communityId", communityId).addValue("userId", authorUserId),
                String.class
            );
            return StringUtils.hasText(roleCd) ? roleCd : "member";
        } catch (EmptyResultDataAccessException ex) {
            return "member";
        }
    }

    private boolean exists(String sql, MapSqlParameterSource params) {
        Integer count = namedJdbc.queryForObject(sql, params, Integer.class);
        return count != null && count > 0;
    }

    private String normalizeTodoStatus(String statusCd) {
        String normalized = safeLower(statusCd);
        return switch (normalized) {
            case "in_progress", "done" -> normalized;
            default -> "requested";
        };
    }

    private String yesNo(String value) {
        return yesNo(value, false);
    }

    private String yesNo(String value, boolean defaultYes) {
        if (!StringUtils.hasText(value)) {
            return defaultYes ? "Y" : "N";
        }
        return "Y".equalsIgnoreCase(value) ? "Y" : "N";
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private Integer getInt(ResultSet rs, String columnLabel) throws SQLException {
        int value = rs.getInt(columnLabel);
        return rs.wasNull() ? null : value;
    }

    private Long getLong(ResultSet rs, String columnLabel) throws SQLException {
        long value = rs.getLong(columnLabel);
        return rs.wasNull() ? null : value;
    }

    private <T> List<T> safeList(List<T> source) {
        return source == null ? List.of() : source;
    }

}
