package kr.or.ddit.calendar.workspace.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import kr.or.ddit.calendar.workspace.dto.CalendarWorkspaceEventDTO;
import kr.or.ddit.calendar.workspace.dto.CalendarWorkspaceQuery;
import kr.or.ddit.calendar.workspace.dto.CalendarWorkspaceResponse;
import kr.or.ddit.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CalendarWorkspaceService {

    private static final Set<String> ALL_SOURCE_GROUPS = Set.of("my", "org", "community", "subscription", "todo", "team");

    private final NamedParameterJdbcTemplate namedJdbc;

    public CalendarWorkspaceResponse readWorkspaceEvents(CalendarWorkspaceQuery query, CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getRealUser() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        LocalDate anchorDate = query.getAnchorDate() == null ? LocalDate.now() : query.getAnchorDate();
        LocalDate rangeStartDate = query.getStartDate();
        LocalDate rangeEndDate = query.getEndDate();
        String view = normalizeView(query.getView());

        if (rangeStartDate == null || rangeEndDate == null) {
            switch (view) {
                case "week" -> {
                    rangeStartDate = anchorDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
                    rangeEndDate = rangeStartDate.plusDays(6);
                }
                case "query", "list", "month" -> {
                    rangeStartDate = anchorDate.withDayOfMonth(1);
                    rangeEndDate = anchorDate.withDayOfMonth(anchorDate.lengthOfMonth());
                }
                default -> throw new IllegalArgumentException("지원하지 않는 보기입니다.");
            }
        }

        if (rangeEndDate.isBefore(rangeStartDate)) {
            LocalDate swap = rangeStartDate;
            rangeStartDate = rangeEndDate;
            rangeEndDate = swap;
        }

        LocalDateTime rangeStart = rangeStartDate.atStartOfDay();
        LocalDateTime rangeEndExclusive = rangeEndDate.plusDays(1).atStartOfDay();
        String userId = userDetails.getUsername();
        String deptId = userDetails.getRealUser().getDeptId();

        Set<String> sourceGroups = parseSourceGroups(query.getSourceGroups());
        List<CalendarWorkspaceEventDTO> items = new ArrayList<>();

        if (sourceGroups.contains("my")) {
            items.addAll(readMyEvents(userId, rangeStart, rangeEndExclusive));
        }
        if (sourceGroups.contains("org")) {
            items.addAll(readDepartmentScheduleEvents(userId, deptId, rangeStart, rangeEndExclusive));
            items.addAll(readDepartmentFeedEvents(userId, deptId, rangeStart, rangeEndExclusive));
        }
        if (sourceGroups.contains("community")) {
            items.addAll(readCommunityScheduleEvents(userId, rangeStart, rangeEndExclusive));
        }
        if (sourceGroups.contains("subscription")) {
            items.addAll(readSubscriptionEvents(userId, rangeStart, rangeEndExclusive));
        }
        if (sourceGroups.contains("todo")) {
            items.addAll(readTodoEvents(userId, rangeStart, rangeEndExclusive));
        }
        if (sourceGroups.contains("team")) {
            items.addAll(readTeamEvents(userId, rangeStart, rangeEndExclusive));
        }

        List<CalendarWorkspaceEventDTO> filtered = items.stream()
            .filter(item -> matchesText(item, query.getQ()))
            .filter(item -> matchesCommunity(item, query.getCommunityId()))
            .filter(item -> matchesProject(item, query.getProjectId()))
            .filter(item -> matchesOwner(item, query.getOwnerUserId()))
            .sorted(Comparator
                .comparing(CalendarWorkspaceEventDTO::getStartDt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(CalendarWorkspaceEventDTO::getTitle, Comparator.nullsLast(String::compareToIgnoreCase))
                .thenComparing(CalendarWorkspaceEventDTO::getEventKey, Comparator.nullsLast(String::compareToIgnoreCase)))
            .collect(Collectors.toList());

        return CalendarWorkspaceResponse.builder()
            .startDate(rangeStartDate)
            .endDate(rangeEndDate)
            .items(filtered)
            .build();
    }

    private List<CalendarWorkspaceEventDTO> readMyEvents(String userId, LocalDateTime rangeStart, LocalDateTime rangeEndExclusive) {
        String sql =
            "SELECT US.USER_SCHD_ID AS EVENT_KEY, 'user_schedule' AS SOURCE_CD, 'my' AS SOURCE_GROUP_CD, " +
            "       US.SCHD_TTL AS TITLE, US.SCHD_STRT_DT AS START_DT, US.SCHD_END_DT AS END_DT, US.ALLDAY AS ALLDAY_YN, " +
            "       US.USER_SCHD_EXPLN AS DESCRIPTION, US.USER_ID AS OWNER_USER_ID, U.USER_NM AS OWNER_USER_NM, " +
            "       U.DEPT_ID AS DEPT_ID, D.DEPT_NM AS DEPT_NM, NULL::BIGINT AS COMMUNITY_ID, NULL::VARCHAR AS COMMUNITY_NM, " +
            "       NULL::VARCHAR AS PST_ID, NULL::VARCHAR AS BIZ_ID, NULL::VARCHAR AS PROJECT_NM, NULL::BIGINT AS TODO_ID, " +
            "       '#ec4899' AS COLOR_CD, 'my_schedule' AS SOURCE_LABEL, 'personal_schedule' AS TYPE_LABEL, " +
            "       TRUE AS CAN_EDIT, TRUE AS CAN_DELETE, NULL::VARCHAR AS DETAIL_HREF, " +
            "       NULL::VARCHAR AS PLACE_TEXT, NULL::VARCHAR AS PLACE_URL, NULL::VARCHAR AS REPEAT_RULE, " +
            "       NULL::CHAR AS DONE_YN, NULL::VARCHAR AS STATUS_LABEL " +
            "FROM USER_SCHEDULE US " +
            "JOIN USERS U ON U.USER_ID = US.USER_ID " +
            "LEFT JOIN DEPARTMENT D ON D.DEPT_ID = U.DEPT_ID " +
            "WHERE US.USER_ID = :userId " +
            "  AND COALESCE(US.DEL_YN, 'N') = 'N' " +
            "  AND COALESCE(US.SCHD_END_DT, US.SCHD_STRT_DT) >= :rangeStart " +
            "  AND US.SCHD_STRT_DT < :rangeEndExclusive";
        return namedJdbc.query(sql, baseParams(userId, rangeStart, rangeEndExclusive), this::mapEvent);
    }

    private List<CalendarWorkspaceEventDTO> readDepartmentScheduleEvents(String userId, String deptId, LocalDateTime rangeStart, LocalDateTime rangeEndExclusive) {
        if (!StringUtils.hasText(deptId)) {
            return List.of();
        }
        String sql =
            "SELECT DS.DEPT_SCHD_ID AS EVENT_KEY, 'dept_schedule' AS SOURCE_CD, 'org' AS SOURCE_GROUP_CD, " +
            "       DS.SCHD_TTL AS TITLE, DS.SCHD_STRT_DT AS START_DT, DS.SCHD_END_DT AS END_DT, DS.ALLDAY AS ALLDAY_YN, " +
            "       DS.DEPT_SCHD_EXPLN AS DESCRIPTION, DS.DEPT_SCHD_CRT_USER_ID AS OWNER_USER_ID, U.USER_NM AS OWNER_USER_NM, " +
            "       DS.DEPT_ID AS DEPT_ID, D.DEPT_NM AS DEPT_NM, NULL::BIGINT AS COMMUNITY_ID, NULL::VARCHAR AS COMMUNITY_NM, " +
            "       NULL::VARCHAR AS PST_ID, NULL::VARCHAR AS BIZ_ID, NULL::VARCHAR AS PROJECT_NM, NULL::BIGINT AS TODO_ID, " +
            "       '#16a34a' AS COLOR_CD, 'department_schedule' AS SOURCE_LABEL, 'department_schedule' AS TYPE_LABEL, " +
            "       (DS.DEPT_SCHD_CRT_USER_ID = :userId) AS CAN_EDIT, (DS.DEPT_SCHD_CRT_USER_ID = :userId) AS CAN_DELETE, NULL::VARCHAR AS DETAIL_HREF, " +
            "       NULL::VARCHAR AS PLACE_TEXT, NULL::VARCHAR AS PLACE_URL, NULL::VARCHAR AS REPEAT_RULE, " +
            "       NULL::CHAR AS DONE_YN, NULL::VARCHAR AS STATUS_LABEL " +
            "FROM DEPARTMENT_SCHEDULE DS " +
            "LEFT JOIN USERS U ON U.USER_ID = DS.DEPT_SCHD_CRT_USER_ID " +
            "LEFT JOIN DEPARTMENT D ON D.DEPT_ID = DS.DEPT_ID " +
            "WHERE DS.DEPT_ID = :deptId " +
            "  AND COALESCE(DS.DEL_YN, 'N') = 'N' " +
            "  AND COALESCE(DS.SCHD_END_DT, DS.SCHD_STRT_DT) >= :rangeStart " +
            "  AND DS.SCHD_STRT_DT < :rangeEndExclusive";
        return namedJdbc.query(sql, baseParams(userId, rangeStart, rangeEndExclusive).addValue("deptId", deptId), this::mapEvent);
    }

    private List<CalendarWorkspaceEventDTO> readDepartmentFeedEvents(String userId, String deptId, LocalDateTime rangeStart, LocalDateTime rangeEndExclusive) {
        if (!StringUtils.hasText(deptId)) {
            return List.of();
        }
        String sql =
            "WITH RECURSIVE DEPT_TREE AS ( " +
            "    SELECT DEPT_ID, UP_DEPT_ID FROM DEPARTMENT WHERE DEPT_ID = :deptId " +
            "    UNION ALL " +
            "    SELECT D.DEPT_ID, D.UP_DEPT_ID FROM DEPARTMENT D JOIN DEPT_TREE DT ON D.UP_DEPT_ID = DT.DEPT_ID " +
            ") " +
            "SELECT FD.EVENT_ID AS EVENT_KEY, 'dept_feed' AS SOURCE_CD, 'org' AS SOURCE_GROUP_CD, " +
            "       FD.TITLE, FD.START_DT, FD.END_DT, FD.ALLDAY AS ALLDAY_YN, FD.DESCRIPTION, FD.USER_ID AS OWNER_USER_ID, FD.USER_NM AS OWNER_USER_NM, " +
            "       FD.DEPT_ID, D.DEPT_NM, NULL::BIGINT AS COMMUNITY_ID, NULL::VARCHAR AS COMMUNITY_NM, " +
            "       NULL::VARCHAR AS PST_ID, NULL::VARCHAR AS BIZ_ID, NULL::VARCHAR AS PROJECT_NM, NULL::BIGINT AS TODO_ID, " +
            "       '#22c55e' AS COLOR_CD, 'department_feed' AS SOURCE_LABEL, 'org_feed_schedule' AS TYPE_LABEL, " +
            "       FALSE AS CAN_EDIT, FALSE AS CAN_DELETE, NULL::VARCHAR AS DETAIL_HREF, " +
            "       NULL::VARCHAR AS PLACE_TEXT, NULL::VARCHAR AS PLACE_URL, NULL::VARCHAR AS REPEAT_RULE, " +
            "       NULL::CHAR AS DONE_YN, NULL::VARCHAR AS STATUS_LABEL " +
            "FROM FULLCALENDAR_DEPT FD " +
            "LEFT JOIN DEPARTMENT D ON D.DEPT_ID = FD.DEPT_ID " +
            "WHERE (FD.USER_ID = :userId OR EXISTS (SELECT 1 FROM DEPT_TREE DT WHERE DT.DEPT_ID = FD.DEPT_ID)) " +
            "  AND COALESCE(FD.END_DT, FD.START_DT) >= :rangeStart " +
            "  AND FD.START_DT < :rangeEndExclusive";
        return namedJdbc.query(sql, baseParams(userId, rangeStart, rangeEndExclusive).addValue("deptId", deptId), this::mapEvent);
    }

    private List<CalendarWorkspaceEventDTO> readCommunityScheduleEvents(String userId, LocalDateTime rangeStart, LocalDateTime rangeEndExclusive) {
        String sql =
            "SELECT B.PST_ID AS EVENT_KEY, 'community_schedule' AS SOURCE_CD, 'community' AS SOURCE_GROUP_CD, " +
            "       B.PST_TTL AS TITLE, BS.START_DT, BS.END_DT, BS.ALLDAY_YN AS ALLDAY_YN, B.CONTENTS AS DESCRIPTION, " +
            "       B.CRT_USER_ID AS OWNER_USER_ID, U.USER_NM AS OWNER_USER_NM, U.DEPT_ID AS DEPT_ID, D.DEPT_NM AS DEPT_NM, " +
            "       B.COMMUNITY_ID, C.COMMUNITY_NM, B.PST_ID, NULL::VARCHAR AS BIZ_ID, NULL::VARCHAR AS PROJECT_NM, NULL::BIGINT AS TODO_ID, " +
            "       '#3b82f6' AS COLOR_CD, 'community_schedule' AS SOURCE_LABEL, 'community_schedule' AS TYPE_LABEL, " +
            "       FALSE AS CAN_EDIT, FALSE AS CAN_DELETE, ('/board?communityId=' || B.COMMUNITY_ID || '&postId=' || B.PST_ID) AS DETAIL_HREF, " +
            "       BS.PLACE_TEXT, BS.PLACE_URL, BS.REPEAT_RULE, NULL::CHAR AS DONE_YN, NULL::VARCHAR AS STATUS_LABEL " +
            "FROM BOARD B " +
            "JOIN BOARD_SCHEDULE BS ON BS.PST_ID = B.PST_ID " +
            "LEFT JOIN USERS U ON U.USER_ID = B.CRT_USER_ID " +
            "LEFT JOIN DEPARTMENT D ON D.DEPT_ID = U.DEPT_ID " +
            "LEFT JOIN COMMUNITY C ON C.COMMUNITY_ID = B.COMMUNITY_ID " +
            "WHERE COALESCE(B.DEL_YN, 'N') = 'N' " +
            "  AND COALESCE(B.PST_TYPE_CD, 'story') = 'schedule' " +
            "  AND B.COMMUNITY_ID IS NOT NULL " +
            "  AND COALESCE(BS.END_DT, BS.START_DT) >= :rangeStart " +
            "  AND BS.START_DT < :rangeEndExclusive " +
            "  AND ( " +
            "        B.CRT_USER_ID = :userId " +
            "        OR EXISTS (SELECT 1 FROM BOARD_SHARE_RECIPIENT SR WHERE SR.PST_ID = B.PST_ID AND SR.RECIPIENT_USER_ID = :userId) " +
            "        OR ( " +
            "            (COALESCE(B.PUBLISH_STATE_CD, 'published') <> 'scheduled' OR B.RESERVED_PUBLISH_DT IS NULL OR B.RESERVED_PUBLISH_DT <= CURRENT_TIMESTAMP) " +
            "            AND LOWER(COALESCE(B.VISIBILITY_CD, 'community')) <> 'private' " +
            "            AND EXISTS (SELECT 1 FROM COMMUNITY_MEMBER CM WHERE CM.COMMUNITY_ID = B.COMMUNITY_ID AND CM.USER_ID = :userId AND CM.STATUS_CD = 'active') " +
            "            AND ( " +
            "                LOWER(COALESCE(C.COMMUNITY_TYPE_CD, 'general')) <> 'notice' " +
            "                OR LOWER(COALESCE(B.COMMUNITY_AUTHOR_ROLE_CD, 'member')) IN ('owner', 'operator') " +
            "                OR EXISTS ( " +
            "                    SELECT 1 FROM COMMUNITY_MEMBER CM2 " +
            "                    WHERE CM2.COMMUNITY_ID = B.COMMUNITY_ID " +
            "                      AND CM2.USER_ID = :userId " +
            "                      AND CM2.STATUS_CD = 'active' " +
            "                      AND CM2.ROLE_CD IN ('owner', 'operator')" +
            "                ) " +
            "            ) " +
            "        ) " +
            "      )";
        return namedJdbc.query(sql, baseParams(userId, rangeStart, rangeEndExclusive), this::mapEvent);
    }

    private List<CalendarWorkspaceEventDTO> readSubscriptionEvents(String userId, LocalDateTime rangeStart, LocalDateTime rangeEndExclusive) {
        String sql =
            "SELECT ('sub-' || US.USER_SCHD_ID) AS EVENT_KEY, 'subscription_schedule' AS SOURCE_CD, 'subscription' AS SOURCE_GROUP_CD, " +
            "       US.SCHD_TTL AS TITLE, US.SCHD_STRT_DT AS START_DT, US.SCHD_END_DT AS END_DT, US.ALLDAY AS ALLDAY_YN, " +
            "       US.USER_SCHD_EXPLN AS DESCRIPTION, US.USER_ID AS OWNER_USER_ID, U.USER_NM AS OWNER_USER_NM, " +
            "       U.DEPT_ID AS DEPT_ID, D.DEPT_NM AS DEPT_NM, NULL::BIGINT AS COMMUNITY_ID, NULL::VARCHAR AS COMMUNITY_NM, " +
            "       NULL::VARCHAR AS PST_ID, NULL::VARCHAR AS BIZ_ID, NULL::VARCHAR AS PROJECT_NM, NULL::BIGINT AS TODO_ID, " +
            "       '#84cc16' AS COLOR_CD, 'subscription_schedule' AS SOURCE_LABEL, 'favorite_user_schedule' AS TYPE_LABEL, " +
            "       FALSE AS CAN_EDIT, FALSE AS CAN_DELETE, NULL::VARCHAR AS DETAIL_HREF, " +
            "       NULL::VARCHAR AS PLACE_TEXT, NULL::VARCHAR AS PLACE_URL, NULL::VARCHAR AS REPEAT_RULE, " +
            "       NULL::CHAR AS DONE_YN, NULL::VARCHAR AS STATUS_LABEL " +
            "FROM USER_FAVORITE_USER F " +
            "JOIN USER_SCHEDULE US ON US.USER_ID = F.TARGET_USER_ID " +
            "JOIN USERS U ON U.USER_ID = US.USER_ID " +
            "LEFT JOIN DEPARTMENT D ON D.DEPT_ID = U.DEPT_ID " +
            "WHERE F.USER_ID = :userId " +
            "  AND COALESCE(US.DEL_YN, 'N') = 'N' " +
            "  AND COALESCE(US.SCHD_END_DT, US.SCHD_STRT_DT) >= :rangeStart " +
            "  AND US.SCHD_STRT_DT < :rangeEndExclusive";
        return namedJdbc.query(sql, baseParams(userId, rangeStart, rangeEndExclusive), this::mapEvent);
    }

    private List<CalendarWorkspaceEventDTO> readTodoEvents(String userId, LocalDateTime rangeStart, LocalDateTime rangeEndExclusive) {
        String sql =
            "SELECT ('todo-' || T.TODO_ID) AS EVENT_KEY, 'user_todo' AS SOURCE_CD, 'todo' AS SOURCE_GROUP_CD, " +
            "       T.TODO_TTL AS TITLE, T.DUE_DT AS START_DT, T.DUE_DT AS END_DT, 'N' AS ALLDAY_YN, " +
            "       T.TODO_CN AS DESCRIPTION, T.USER_ID AS OWNER_USER_ID, U.USER_NM AS OWNER_USER_NM, " +
            "       U.DEPT_ID AS DEPT_ID, D.DEPT_NM AS DEPT_NM, NULL::BIGINT AS COMMUNITY_ID, NULL::VARCHAR AS COMMUNITY_NM, " +
            "       NULL::VARCHAR AS PST_ID, NULL::VARCHAR AS BIZ_ID, NULL::VARCHAR AS PROJECT_NM, T.TODO_ID AS TODO_ID, " +
            "       '#38bdf8' AS COLOR_CD, 'user_todo' AS SOURCE_LABEL, 'todo' AS TYPE_LABEL, " +
            "       FALSE AS CAN_EDIT, FALSE AS CAN_DELETE, NULL::VARCHAR AS DETAIL_HREF, " +
            "       NULL::VARCHAR AS PLACE_TEXT, NULL::VARCHAR AS PLACE_URL, NULL::VARCHAR AS REPEAT_RULE, " +
            "       T.DONE_YN AS DONE_YN, CASE WHEN COALESCE(T.DONE_YN, 'N') = 'Y' THEN 'done' ELSE 'open' END AS STATUS_LABEL " +
            "FROM USER_TODO T " +
            "JOIN USERS U ON U.USER_ID = T.USER_ID " +
            "LEFT JOIN DEPARTMENT D ON D.DEPT_ID = U.DEPT_ID " +
            "WHERE T.USER_ID = :userId " +
            "  AND T.DUE_DT IS NOT NULL " +
            "  AND T.DUE_DT >= :rangeStart " +
            "  AND T.DUE_DT < :rangeEndExclusive";
        return namedJdbc.query(sql, baseParams(userId, rangeStart, rangeEndExclusive), this::mapEvent);
    }

    private List<CalendarWorkspaceEventDTO> readTeamEvents(String userId, LocalDateTime rangeStart, LocalDateTime rangeEndExclusive) {
        String sql =
            "SELECT FT.EVENT_ID AS EVENT_KEY, 'team_schedule' AS SOURCE_CD, 'team' AS SOURCE_GROUP_CD, " +
            "       FT.TITLE, FT.START_DT, FT.END_DT, FT.ALLDAY AS ALLDAY_YN, FT.DESCRIPTION, FT.USER_ID AS OWNER_USER_ID, FT.USER_NM AS OWNER_USER_NM, " +
            "       NULL::VARCHAR AS DEPT_ID, NULL::VARCHAR AS DEPT_NM, NULL::BIGINT AS COMMUNITY_ID, NULL::VARCHAR AS COMMUNITY_NM, " +
            "       NULL::VARCHAR AS PST_ID, FT.BIZ_ID, P.BIZ_NM AS PROJECT_NM, NULL::BIGINT AS TODO_ID, " +
            "       '#f97316' AS COLOR_CD, 'team_schedule' AS SOURCE_LABEL, 'project_schedule' AS TYPE_LABEL, " +
            "       FALSE AS CAN_EDIT, FALSE AS CAN_DELETE, NULL::VARCHAR AS DETAIL_HREF, " +
            "       NULL::VARCHAR AS PLACE_TEXT, NULL::VARCHAR AS PLACE_URL, NULL::VARCHAR AS REPEAT_RULE, " +
            "       NULL::CHAR AS DONE_YN, NULL::VARCHAR AS STATUS_LABEL " +
            "FROM FULLCALENDAR_TEAM FT " +
            "LEFT JOIN PROJECT P ON P.BIZ_ID = FT.BIZ_ID " +
            "WHERE FT.USER_ID = :userId " +
            "  AND COALESCE(FT.END_DT, FT.START_DT) >= :rangeStart " +
            "  AND FT.START_DT < :rangeEndExclusive";
        return namedJdbc.query(sql, baseParams(userId, rangeStart, rangeEndExclusive), this::mapEvent);
    }

    private MapSqlParameterSource baseParams(String userId, LocalDateTime rangeStart, LocalDateTime rangeEndExclusive) {
        return new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("rangeStart", Timestamp.valueOf(rangeStart))
            .addValue("rangeEndExclusive", Timestamp.valueOf(rangeEndExclusive));
    }

    private CalendarWorkspaceEventDTO mapEvent(ResultSet rs, int rowNum) throws SQLException {
        CalendarWorkspaceEventDTO item = new CalendarWorkspaceEventDTO();
        item.setEventKey(rs.getString("EVENT_KEY"));
        item.setSourceCd(rs.getString("SOURCE_CD"));
        item.setSourceGroupCd(rs.getString("SOURCE_GROUP_CD"));
        item.setTitle(rs.getString("TITLE"));
        item.setStartDt(toLocalDateTime(rs.getTimestamp("START_DT")));
        item.setEndDt(toLocalDateTime(rs.getTimestamp("END_DT")));
        item.setAlldayYn(rs.getString("ALLDAY_YN"));
        item.setDescription(rs.getString("DESCRIPTION"));
        item.setOwnerUserId(rs.getString("OWNER_USER_ID"));
        item.setOwnerUserNm(rs.getString("OWNER_USER_NM"));
        item.setDeptId(rs.getString("DEPT_ID"));
        item.setDeptNm(rs.getString("DEPT_NM"));
        item.setCommunityId(getLong(rs, "COMMUNITY_ID"));
        item.setCommunityNm(rs.getString("COMMUNITY_NM"));
        item.setPstId(rs.getString("PST_ID"));
        item.setBizId(rs.getString("BIZ_ID"));
        item.setProjectNm(rs.getString("PROJECT_NM"));
        item.setTodoId(getLong(rs, "TODO_ID"));
        item.setColorCd(rs.getString("COLOR_CD"));
        item.setSourceLabel(rs.getString("SOURCE_LABEL"));
        item.setTypeLabel(rs.getString("TYPE_LABEL"));
        item.setCanEdit(rs.getBoolean("CAN_EDIT"));
        item.setCanDelete(rs.getBoolean("CAN_DELETE"));
        item.setDetailHref(rs.getString("DETAIL_HREF"));
        item.setPlaceText(rs.getString("PLACE_TEXT"));
        item.setPlaceUrl(rs.getString("PLACE_URL"));
        item.setRepeatRule(rs.getString("REPEAT_RULE"));
        item.setDoneYn(rs.getString("DONE_YN"));
        item.setStatusLabel(rs.getString("STATUS_LABEL"));
        return item;
    }

    private boolean matchesText(CalendarWorkspaceEventDTO item, String query) {
        if (!StringUtils.hasText(query)) {
            return true;
        }
        String keyword = query.trim().toLowerCase(Locale.ROOT);
        return contains(item.getTitle(), keyword)
            || contains(item.getDescription(), keyword)
            || contains(item.getOwnerUserNm(), keyword)
            || contains(item.getCommunityNm(), keyword)
            || contains(item.getProjectNm(), keyword)
            || contains(item.getPlaceText(), keyword);
    }

    private boolean matchesCommunity(CalendarWorkspaceEventDTO item, Long communityId) {
        return communityId == null || Objects.equals(item.getCommunityId(), communityId);
    }

    private boolean matchesProject(CalendarWorkspaceEventDTO item, String projectId) {
        return !StringUtils.hasText(projectId) || Objects.equals(item.getBizId(), projectId);
    }

    private boolean matchesOwner(CalendarWorkspaceEventDTO item, String ownerUserId) {
        return !StringUtils.hasText(ownerUserId) || Objects.equals(item.getOwnerUserId(), ownerUserId);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private Set<String> parseSourceGroups(String sourceGroups) {
        if (!StringUtils.hasText(sourceGroups)) {
            return ALL_SOURCE_GROUPS;
        }
        Set<String> parsed = new LinkedHashSet<>();
        for (String token : sourceGroups.split(",")) {
            String normalized = token == null ? "" : token.trim().toLowerCase(Locale.ROOT);
            if (ALL_SOURCE_GROUPS.contains(normalized)) {
                parsed.add(normalized);
            }
        }
        return parsed.isEmpty() ? ALL_SOURCE_GROUPS : parsed;
    }

    private String normalizeView(String view) {
        String normalized = view == null ? "" : view.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "week", "list", "query" -> normalized;
            default -> "month";
        };
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private Long getLong(ResultSet rs, String columnLabel) throws SQLException {
        long value = rs.getLong(columnLabel);
        return rs.wasNull() ? null : value;
    }
}
