package kr.or.ddit.dashboard.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import kr.or.ddit.dashboard.dto.CntCardDTO;
import kr.or.ddit.dashboard.dto.CurrentProjectDTO;
import kr.or.ddit.dashboard.dto.DashboardCategoryRecommendationDTO;
import kr.or.ddit.dashboard.dto.DashboardFavoriteUserDTO;
import kr.or.ddit.dashboard.dto.DashboardFeedItemDTO;
import kr.or.ddit.dashboard.dto.DashboardFeedPreferenceDTO;
import kr.or.ddit.dashboard.dto.DashboardProfileDTO;
import kr.or.ddit.dashboard.dto.DashboardTodoDTO;
import kr.or.ddit.dashboard.dto.DashboardWidgetItemDTO;
import kr.or.ddit.dashboard.dto.DepartStatusDTO;
import kr.or.ddit.dashboard.dto.RecentBoardDTO;
import kr.or.ddit.dashboard.dto.TodayScheduleDTO;
import kr.or.ddit.dashboard.service.DashboardService;
import kr.or.ddit.mybatis.mapper.AuthorizationDocumentMapper;
import kr.or.ddit.mybatis.mapper.BoardCommentMapper;
import kr.or.ddit.mybatis.mapper.BoardMapper;
import kr.or.ddit.mybatis.mapper.DashboardPreferenceMapper;
import kr.or.ddit.mybatis.mapper.DepartmentMapper;
import kr.or.ddit.mybatis.mapper.DepartmentScheduleMapper;
import kr.or.ddit.mybatis.mapper.EmailBoxMapper;
import kr.or.ddit.mybatis.mapper.MainTaskMapper;
import kr.or.ddit.mybatis.mapper.ProjectMapper;
import kr.or.ddit.mybatis.mapper.TimeAndAttendanceMapper;
import kr.or.ddit.mybatis.mapper.UserHistoryMapper;
import kr.or.ddit.mybatis.mapper.UserScheduleMapper;
import kr.or.ddit.mybatis.mapper.UsersMapper;
import kr.or.ddit.security.CustomUserDetails;
import kr.or.ddit.vo.BoardCommentVO;
import kr.or.ddit.vo.BoardVO;
import kr.or.ddit.vo.DepartmentScheduleVO;
import kr.or.ddit.vo.DepartmentVO;
import kr.or.ddit.vo.MainTaskVO;
import kr.or.ddit.vo.ProjectVO;
import kr.or.ddit.vo.TimeAndAttendanceVO;
import kr.or.ddit.vo.UserHistoryVO;
import kr.or.ddit.vo.UserScheduleVO;
import kr.or.ddit.vo.UsersVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardServiceImpl implements DashboardService {

    private static final int FEED_PAGE_SIZE = 10;
    private static final int PROFILE_ACTIVITY_LIMIT = 5;
    private static final String NOTICE_CATEGORY = "F101";
    private static final Set<String> BOARD_CATEGORY_CODES = Set.of("F101", "F102", "F103", "F104", "F105", "F106");
    private static final Set<String> INTEREST_CATEGORY_CODES = Set.of("F102", "F103", "F104", "F105", "F106");
    private static final Set<String> FEED_SCOPES = Set.of("all", "unread", "saved", "my-posts", "commented", "activity");

    private static final Map<String, String> BOARD_CATEGORY_LABELS = Map.of(
        "F101", "공지",
        "F102", "동호회",
        "F103", "경조사",
        "F104", "사내소식",
        "F105", "건의사항",
        "F106", "기타"
    );

    private static final Map<String, String> PROJECT_STATUS_LABELS = Map.of(
        "B301", "준비",
        "B302", "진행",
        "B303", "보류",
        "B304", "완료",
        "B305", "취소"
    );

    private static final Map<String, String> TASK_STATUS_LABELS = Map.of(
        "B401", "미시작",
        "B402", "진행중",
        "B403", "보류",
        "B404", "완료",
        "B405", "삭제"
    );

    private final TimeAndAttendanceMapper tAndAMapper;
    private final MainTaskMapper mainTaskMapper;
    private final AuthorizationDocumentMapper approvalMapper;
    private final EmailBoxMapper emailBoxMapper;
    private final BoardMapper boardMapper;
    private final BoardCommentMapper boardCommentMapper;
    private final ProjectMapper projectMapper;
    private final UsersMapper userMapper;
    private final DepartmentMapper departmentMapper;
    private final DepartmentScheduleMapper departmentScheduleMapper;
    private final UserScheduleMapper userScheduleMapper;
    private final DashboardPreferenceMapper dashboardPreferenceMapper;
    private final UserHistoryMapper userHistoryMapper;

    @Override
    public Map<String, Object> getAttendanceRecord() {
        String userId = requireCurrentUserId();
        String workYmd = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        TimeAndAttendanceVO attendance = tAndAMapper.selectTimeAndAttendance(userId, workYmd);

        Map<String, Object> record = new LinkedHashMap<>();
        if (attendance == null) {
            return record;
        }

        record.put("workStartTime", formatTimeOnly(attendance.getWorkBgngDt()));
        record.put("workEndTime", formatTimeOnly(attendance.getWorkEndDt()));
        record.put("workinghours", attendance.getWorkHr() == null ? null : attendance.getWorkHr().toString());
        return record;
    }

    @Override
    public CntCardDTO getCntCard() {
        UsersVO currentUser = requireCurrentUser();
        String userId = currentUser.getUserId();

        int mainTaskCnt = (int) mainTaskMapper.selectMyTaskListNonPaging(userId).stream()
            .filter(task -> "B402".equals(task.getTaskSttsCd()))
            .count();

        int waitApprovalCnt = approvalMapper.countMyInboxCombined(Map.of("userId", userId));
        int weekScheduleCnt = countWeekSchedules(currentUser);
        int unreadMailCnt = 0;
        try {
            unreadMailCnt = emailBoxMapper.selectUnreadEmailCount(userId, "G101");
        } catch (Exception ex) {
            log.warn("대시보드 메일 카운트를 불러오지 못했습니다. userId={}", userId, ex);
        }

        return new CntCardDTO(mainTaskCnt, waitApprovalCnt, weekScheduleCnt, unreadMailCnt);
    }

    @Override
    public List<TodayScheduleDTO> getTodayScheDuleList() {
        UsersVO currentUser = requireCurrentUser();
        LocalDate today = LocalDate.now();
        Map<String, UsersVO> userCache = new HashMap<>();
        userCache.put(currentUser.getUserId(), currentUser);

        List<TodayScheduleDTO> items = new ArrayList<>();

        for (UserScheduleVO schedule : userScheduleMapper.selectUserScheduleListByUserId(currentUser.getUserId())) {
            if (!isScheduleOnDate(schedule.getSchdStrtDt(), schedule.getSchdEndDt(), schedule.getDelYn(), today)) {
                continue;
            }
            items.add(new TodayScheduleDTO(
                schedule.getUserSchdId(),
                schedule.getSchdStrtDt(),
                schedule.getSchdEndDt(),
                schedule.getSchdTtl(),
                "개인일정",
                currentUser.getUserNm(),
                currentUser.getJbgdNm(),
                currentUser.getDeptNm(),
                defaultString(schedule.getUserSchdExpln(), "")
            ));
        }

        for (DepartmentScheduleVO schedule : departmentScheduleMapper.selectDepartmentScheduleListByDeptId(currentUser.getDeptId())) {
            if (!isScheduleOnDate(schedule.getSchdStrtDt(), schedule.getSchdEndDt(), schedule.getDelYn(), today)) {
                continue;
            }
            UsersVO author = readUser(schedule.getDeptSchdCrtUserId(), userCache);
            items.add(new TodayScheduleDTO(
                schedule.getDeptSchdId(),
                schedule.getSchdStrtDt(),
                schedule.getSchdEndDt(),
                schedule.getSchdTtl(),
                "부서일정",
                author == null ? schedule.getDeptSchdCrtUserId() : defaultString(author.getUserNm(), schedule.getDeptSchdCrtUserId()),
                author == null ? null : author.getJbgdNm(),
                author == null ? currentUser.getDeptNm() : author.getDeptNm(),
                defaultString(schedule.getDeptSchdExpln(), "")
            ));
        }

        items.sort(Comparator.comparing(TodayScheduleDTO::getStartTime, Comparator.nullsLast(Comparator.naturalOrder())));
        return items;
    }

    @Override
    public List<CurrentProjectDTO> getCurrentProjectList() {
        return projectMapper.selectMyProjectListNonPaging(requireCurrentUserId()).stream()
            .filter(this::isInProgressProject)
            .sorted(Comparator.comparing(ProjectVO::getEndBizDt, Comparator.nullsLast(Comparator.naturalOrder())))
            .limit(5)
            .map(project -> new CurrentProjectDTO(
                project.getBizId(),
                project.getBizNm(),
                project.getStrtBizDt(),
                project.getEndBizDt(),
                project.getBizPrgrs()
            ))
            .toList();
    }

    @Override
    public List<RecentBoardDTO> getRecentBoardList() {
        Map<String, UsersVO> userCache = new HashMap<>();

        return loadAllBoards().stream()
            .sorted(Comparator.comparing(BoardVO::getFrstCrtDt, Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(5)
            .map(board -> new RecentBoardDTO(
                board.getPstId(),
                board.getPstTtl(),
                readUser(board.getCrtUserId(), userCache),
                board.getFrstCrtDt()
            ))
            .toList();
    }

    @Override
    public DepartStatusDTO getDepartStatusCnt() {
        UsersVO currentUser = requireCurrentUser();

        int total = 0;
        int working = 0;
        int vacation = 0;
        int businessTrip = 0;

        for (UsersVO member : userMapper.selectUsersByDept(currentUser.getDeptId())) {
            if ("Y".equalsIgnoreCase(member.getRsgntnYn())) {
                continue;
            }
            total++;
            String workStatus = defaultString(member.getWorkSttsCd(), "C103");
            if ("C104".equals(workStatus)) {
                vacation++;
            } else if ("C105".equals(workStatus)) {
                businessTrip++;
            } else {
                working++;
            }
        }

        return new DepartStatusDTO(total, working, vacation, businessTrip);
    }

    @Override
    public Map<String, Object> getFeed(String scope, String category, String deptId, String q, String sort, String view, int page) {
        UsersVO currentUser = requireCurrentUser();
        String currentUserId = currentUser.getUserId();
        String normalizedScope = normalizeScope(scope);
        String normalizedCategory = normalizeCategory(category);
        String normalizedDeptId = isBlank(deptId) ? null : deptId;
        String normalizedSort = normalizeSort(sort);
        String normalizedView = normalizeView(view);
        String keyword = q == null ? "" : q.trim();

        Map<String, UsersVO> userCache = new HashMap<>();
        userCache.put(currentUserId, currentUser);

        Set<String> readPostIds = new LinkedHashSet<>(dashboardPreferenceMapper.selectReadPostIds(currentUserId));
        Set<String> savedPostIds = new LinkedHashSet<>(dashboardPreferenceMapper.selectSavedPostIds(currentUserId));
        Set<String> commentedPostIds = new LinkedHashSet<>(dashboardPreferenceMapper.selectCommentedPostIds(currentUserId));

        List<DashboardFeedItemDTO> allItems = loadFeedItems(currentUser, userCache, readPostIds, savedPostIds, commentedPostIds);
        List<DashboardFeedItemDTO> baseItems = allItems.stream()
            .filter(item -> matchesDept(item, normalizedDeptId))
            .filter(item -> matchesSearch(item, keyword))
            .collect(Collectors.toCollection(ArrayList::new));

        Map<String, Integer> counts = buildFeedCounts(baseItems);

        List<DashboardFeedItemDTO> filteredItems = baseItems.stream()
            .filter(item -> matchesScope(item, normalizedScope))
            .filter(item -> matchesCategory(item, normalizedCategory))
            .collect(Collectors.toCollection(ArrayList::new));

        sortFeedItems(filteredItems, normalizedSort, normalizedScope);

        int currentPage = Math.max(page, 1);
        int totalRecords = filteredItems.size();
        int totalPages = totalRecords == 0 ? 1 : (int) Math.ceil((double) totalRecords / FEED_PAGE_SIZE);
        if (currentPage > totalPages) {
            currentPage = totalPages;
        }

        int fromIndex = Math.max(0, (currentPage - 1) * FEED_PAGE_SIZE);
        int toIndex = Math.min(totalRecords, fromIndex + FEED_PAGE_SIZE);
        List<DashboardFeedItemDTO> pageItems = fromIndex >= totalRecords ? List.of() : filteredItems.subList(fromIndex, toIndex);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("scope", normalizedScope);
        response.put("category", normalizedCategory);
        response.put("deptId", normalizedDeptId);
        response.put("q", keyword);
        response.put("sort", normalizedSort);
        response.put("view", normalizedView);
        response.put("page", currentPage);
        response.put("totalPages", totalPages);
        response.put("totalRecords", totalRecords);
        response.put("items", pageItems);
        response.put("counts", counts);
        response.put("departments", buildDepartmentOptions());
        response.put("favoriteCategories", getFavoriteCategories());
        response.put("preferences", getFeedPreference());
        response.put("recommendationCount", unreadRecommendationCount(currentUserId));
        return response;
    }

    @Override
    public Map<String, Object> getWidgets() {
        UsersVO currentUser = requireCurrentUser();
        String userId = currentUser.getUserId();

        List<DashboardWidgetItemDTO> notices = boardMapper.selectNoticeListNonPaging().stream()
            .sorted(Comparator.comparing(BoardVO::getFrstCrtDt, Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(5)
            .map(board -> DashboardWidgetItemDTO.builder()
                .itemType("notice")
                .title(board.getPstTtl())
                .subtitle(defaultString(board.getUserNm(), board.getCrtUserId()))
                .description(stripHtml(board.getContents()))
                .badge("Y".equalsIgnoreCase(board.getFixedYn()) ? "고정" : "공지")
                .route("/board")
                .createdAt(board.getFrstCrtDt())
                .build())
            .toList();

        List<DashboardWidgetItemDTO> sharedSchedules = departmentScheduleMapper.selectDepartmentScheduleListByDeptId(currentUser.getDeptId()).stream()
            .filter(this::isUpcomingDepartmentSchedule)
            .sorted(Comparator.comparing(DepartmentScheduleVO::getSchdStrtDt, Comparator.nullsLast(Comparator.naturalOrder())))
            .limit(5)
            .map(schedule -> DashboardWidgetItemDTO.builder()
                .itemType("sharedSchedule")
                .title(schedule.getSchdTtl())
                .subtitle(defaultString(currentUser.getDeptNm(), "부서 일정"))
                .description(buildDateRange(schedule.getSchdStrtDt(), schedule.getSchdEndDt(), schedule.getAllday()))
                .badge("전체 일정")
                .route("/calendar")
                .createdAt(schedule.getSchdStrtDt())
                .build())
            .toList();

        List<DashboardWidgetItemDTO> mySchedules = userScheduleMapper.selectUserScheduleListByUserId(userId).stream()
            .filter(this::isUpcomingUserSchedule)
            .sorted(Comparator.comparing(UserScheduleVO::getSchdStrtDt, Comparator.nullsLast(Comparator.naturalOrder())))
            .limit(5)
            .map(schedule -> DashboardWidgetItemDTO.builder()
                .itemType("mySchedule")
                .title(schedule.getSchdTtl())
                .subtitle("나의 일정")
                .description(buildDateRange(schedule.getSchdStrtDt(), schedule.getSchdEndDt(), schedule.getAllday()))
                .badge("개인 일정")
                .route("/calendar")
                .createdAt(schedule.getSchdStrtDt())
                .build())
            .toList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("notices", notices);
        response.put("sharedSchedules", sharedSchedules);
        response.put("mySchedules", mySchedules);
        response.put("todoItems", buildTodoItems(currentUser));
        response.put("quickLinks", buildQuickLinks());
        response.put("favoriteUsers", getFavoriteUsers());
        response.put("recommendationCount", unreadRecommendationCount(userId));
        response.put("approvalCount", approvalMapper.countMyInboxCombined(Map.of("userId", userId)));
        return response;
    }

    @Override
    public DashboardFeedPreferenceDTO getFeedPreference() {
        String userId = requireCurrentUserId();
        DashboardFeedPreferenceDTO preference = dashboardPreferenceMapper.selectPreference(userId);
        if (preference != null) {
            return preference;
        }

        return DashboardFeedPreferenceDTO.builder()
            .userId(userId)
            .defaultScope("all")
            .defaultSort("recent")
            .defaultView("summary")
            .defaultCategory("all")
            .build();
    }

    @Override
    public DashboardFeedPreferenceDTO saveFeedPreference(DashboardFeedPreferenceDTO preference) {
        String userId = requireCurrentUserId();
        DashboardFeedPreferenceDTO target = DashboardFeedPreferenceDTO.builder()
            .userId(userId)
            .defaultScope(normalizeScope(preference == null ? null : preference.getDefaultScope()))
            .defaultSort(normalizeSort(preference == null ? null : preference.getDefaultSort()))
            .defaultView(normalizeView(preference == null ? null : preference.getDefaultView()))
            .defaultCategory(normalizeCategory(preference == null ? null : preference.getDefaultCategory()))
            .lastDeptId(preference == null ? null : preference.getLastDeptId())
            .lastSearchQ(preference == null ? null : trimToNull(preference.getLastSearchQ()))
            .build();

        dashboardPreferenceMapper.upsertPreference(target);
        return getFeedPreference();
    }

    @Override
    public List<String> getFavoriteCategories() {
        String userId = requireCurrentUserId();
        List<String> categories = sanitizeInterestCategories(dashboardPreferenceMapper.selectCategoryCodes(userId));
        return categories.isEmpty() ? List.of("F104") : categories;
    }

    @Override
    public List<String> saveFavoriteCategories(List<String> categoryCodes) {
        String userId = requireCurrentUserId();
        List<String> categories = sanitizeInterestCategories(categoryCodes);

        dashboardPreferenceMapper.deleteCategoryCodes(userId);
        for (String categoryCode : categories) {
            dashboardPreferenceMapper.insertCategoryCode(userId, categoryCode);
        }
        return categories;
    }

    @Override
    public void markBoardRead(String pstId) {
        String userId = requireCurrentUserId();
        ensureBoardExists(pstId);
        dashboardPreferenceMapper.insertReadPost(userId, pstId);
    }

    @Override
    public void savePost(String pstId) {
        String userId = requireCurrentUserId();
        ensureBoardExists(pstId);
        dashboardPreferenceMapper.insertSavedPost(userId, pstId);
    }

    @Override
    public void unsavePost(String pstId) {
        dashboardPreferenceMapper.deleteSavedPost(requireCurrentUserId(), pstId);
    }

    @Override
    public List<DashboardFavoriteUserDTO> getFavoriteUsers() {
        return dashboardPreferenceMapper.selectFavoriteUsers(requireCurrentUserId());
    }

    @Override
    public void addFavoriteUser(String targetUserId) {
        String userId = requireCurrentUserId();
        if (Objects.equals(userId, targetUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "본인은 즐겨찾기할 수 없습니다.");
        }
        ensureUserExists(targetUserId);
        dashboardPreferenceMapper.insertFavoriteUser(userId, targetUserId);
    }

    @Override
    public void removeFavoriteUser(String targetUserId) {
        dashboardPreferenceMapper.deleteFavoriteUser(requireCurrentUserId(), targetUserId);
    }

    @Override
    public List<DashboardTodoDTO> getTodos() {
        return dashboardPreferenceMapper.selectTodos(requireCurrentUserId());
    }

    @Override
    public DashboardTodoDTO createTodo(DashboardTodoDTO todo) {
        String userId = requireCurrentUserId();
        DashboardTodoDTO target = DashboardTodoDTO.builder()
            .userId(userId)
            .targetUserId(trimToNull(todo == null ? null : todo.getTargetUserId()))
            .todoTtl(requireText(todo == null ? null : todo.getTodoTtl(), "할 일 제목이 필요합니다."))
            .todoCn(trimToNull(todo == null ? null : todo.getTodoCn()))
            .doneYn(normalizeYn(todo == null ? null : todo.getDoneYn(), "N"))
            .dueDt(todo == null ? null : todo.getDueDt())
            .build();

        if (!isBlank(target.getTargetUserId())) {
            ensureUserExists(target.getTargetUserId());
        }

        dashboardPreferenceMapper.insertTodo(target);
        return findTodoOrThrow(target.getTodoId());
    }

    @Override
    public DashboardTodoDTO updateTodo(Long todoId, DashboardTodoDTO todo) {
        DashboardTodoDTO current = findTodoOrThrow(todoId);

        current.setTargetUserId(trimToNull(todo == null ? current.getTargetUserId() : todo.getTargetUserId()));
        current.setTodoTtl(requireText(todo == null ? current.getTodoTtl() : todo.getTodoTtl(), "할 일 제목이 필요합니다."));
        current.setTodoCn(trimToNull(todo == null ? current.getTodoCn() : todo.getTodoCn()));
        current.setDoneYn(normalizeYn(todo == null ? current.getDoneYn() : todo.getDoneYn(), current.getDoneYn()));
        current.setDueDt(todo == null || todo.getDueDt() == null ? current.getDueDt() : todo.getDueDt());

        if (!isBlank(current.getTargetUserId())) {
            ensureUserExists(current.getTargetUserId());
        }

        dashboardPreferenceMapper.updateTodo(current);
        return findTodoOrThrow(todoId);
    }

    @Override
    public void deleteTodo(Long todoId) {
        int deleted = dashboardPreferenceMapper.deleteTodo(requireCurrentUserId(), todoId);
        if (deleted == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "할 일을 찾을 수 없습니다.");
        }
    }

    @Override
    public Map<String, Object> getCategoryRecommendations(String box) {
        String userId = requireCurrentUserId();
        String normalizedBox = normalizeRecommendationBox(box);

        List<DashboardCategoryRecommendationDTO> items;
        if ("sent".equals(normalizedBox)) {
            items = dashboardPreferenceMapper.selectSentRecommendations(userId);
        } else if ("all".equals(normalizedBox)) {
            items = new ArrayList<>();
            items.addAll(dashboardPreferenceMapper.selectReceivedRecommendations(userId));
            items.addAll(dashboardPreferenceMapper.selectSentRecommendations(userId));
            items.sort(Comparator.comparing(DashboardCategoryRecommendationDTO::getCrtDt, Comparator.nullsLast(Comparator.reverseOrder())));
        } else {
            items = dashboardPreferenceMapper.selectReceivedRecommendations(userId);
        }

        items.forEach(this::hydrateRecommendationLabel);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("box", normalizedBox);
        response.put("items", items);
        response.put("unreadCount", unreadRecommendationCount(userId));
        return response;
    }

    @Override
    public List<DashboardCategoryRecommendationDTO> createCategoryRecommendations(String targetUserId, List<String> categoryCodes, String message) {
        String userId = requireCurrentUserId();
        if (Objects.equals(userId, targetUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "본인에게는 추천을 보낼 수 없습니다.");
        }

        ensureUserExists(targetUserId);
        List<String> categories = sanitizeInterestCategories(categoryCodes);
        if (categories.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "추천할 분류를 선택해 주세요.");
        }

        List<DashboardCategoryRecommendationDTO> created = new ArrayList<>();
        for (String categoryCode : categories) {
            DashboardCategoryRecommendationDTO recommendation = DashboardCategoryRecommendationDTO.builder()
                .fromUserId(userId)
                .toUserId(targetUserId)
                .bbsCtgrCd(categoryCode)
                .message(trimToNull(message))
                .acceptedYn("N")
                .readYn("N")
                .build();
            dashboardPreferenceMapper.insertCategoryRecommendation(recommendation);
            DashboardCategoryRecommendationDTO saved = dashboardPreferenceMapper.selectRecommendation(recommendation.getRecommendId());
            if (saved != null) {
                hydrateRecommendationLabel(saved);
                created.add(saved);
            }
        }
        return created;
    }

    @Override
    public DashboardCategoryRecommendationDTO updateCategoryRecommendation(Long recommendId, String acceptedYn, String readYn) {
        String userId = requireCurrentUserId();
        DashboardCategoryRecommendationDTO recommendation = dashboardPreferenceMapper.selectRecommendation(recommendId);
        if (recommendation == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "추천 정보를 찾을 수 없습니다.");
        }
        if (!Objects.equals(userId, recommendation.getToUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "대상자만 추천 상태를 변경할 수 있습니다.");
        }

        recommendation.setAcceptedYn(normalizeAccepted(acceptedYn, recommendation.getAcceptedYn()));
        recommendation.setReadYn(normalizeYn(readYn, isBlank(readYn) ? "Y" : readYn));
        dashboardPreferenceMapper.updateCategoryRecommendation(recommendation);

        if ("Y".equalsIgnoreCase(recommendation.getAcceptedYn())) {
            List<String> categories = new ArrayList<>(getFavoriteCategories());
            if (!categories.contains(recommendation.getBbsCtgrCd())) {
                categories.add(recommendation.getBbsCtgrCd());
                saveFavoriteCategories(categories);
            }
        }

        DashboardCategoryRecommendationDTO refreshed = dashboardPreferenceMapper.selectRecommendation(recommendId);
        if (refreshed != null) {
            hydrateRecommendationLabel(refreshed);
        }
        return refreshed;
    }

    @Override
    public DashboardProfileDTO getProfile(String userId) {
        UsersVO targetUser = ensureUserExists(userId);
        String currentUserId = requireCurrentUserId();

        Map<String, UsersVO> userCache = new HashMap<>();
        userCache.put(currentUserId, requireCurrentUser());
        userCache.put(targetUser.getUserId(), targetUser);

        List<BoardVO> recentBoards = loadAllBoards().stream()
            .filter(board -> Objects.equals(board.getCrtUserId(), userId))
            .sorted(Comparator.comparing(BoardVO::getFrstCrtDt, Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(PROFILE_ACTIVITY_LIMIT)
            .toList();

        List<DashboardFeedItemDTO> activities = loadFeedItems(requireCurrentUser(), userCache, Set.of(), Set.of(), Set.of()).stream()
            .filter(item -> "activity".equals(item.getItemType()))
            .filter(item -> Objects.equals(item.getActorUserId(), userId))
            .sorted(Comparator.comparing(DashboardFeedItemDTO::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(PROFILE_ACTIVITY_LIMIT)
            .toList();

        List<UserHistoryVO> histories = userHistoryMapper.selectUserHistoryByUser(userId);
        boolean favorite = dashboardPreferenceMapper.countFavoriteUser(currentUserId, userId) > 0;

        return DashboardProfileDTO.builder()
            .user(targetUser)
            .favorite(favorite)
            .recentBoards(recentBoards)
            .recentActivities(activities)
            .histories(histories)
            .build();
    }

    private int countWeekSchedules(UsersVO currentUser) {
        LocalDate today = LocalDate.now();
        WeekFields weekFields = WeekFields.of(Locale.getDefault());
        LocalDate weekStart = today.with(weekFields.dayOfWeek(), 1);
        LocalDate weekEnd = today.with(weekFields.dayOfWeek(), 7);

        int count = 0;
        for (UserScheduleVO schedule : userScheduleMapper.selectUserScheduleListByUserId(currentUser.getUserId())) {
            if (overlapsWeek(schedule.getSchdStrtDt(), schedule.getSchdEndDt(), schedule.getDelYn(), weekStart, weekEnd)) {
                count++;
            }
        }
        for (DepartmentScheduleVO schedule : departmentScheduleMapper.selectDepartmentScheduleListByDeptId(currentUser.getDeptId())) {
            if (overlapsWeek(schedule.getSchdStrtDt(), schedule.getSchdEndDt(), schedule.getDelYn(), weekStart, weekEnd)) {
                count++;
            }
        }
        return count;
    }

    private List<DashboardFeedItemDTO> loadFeedItems(
        UsersVO currentUser,
        Map<String, UsersVO> userCache,
        Set<String> readPostIds,
        Set<String> savedPostIds,
        Set<String> commentedPostIds
    ) {
        String currentUserId = currentUser.getUserId();
        List<DashboardFeedItemDTO> items = new ArrayList<>();

        for (BoardVO board : loadAllBoards()) {
            items.add(toBoardFeedItem(board, currentUserId, readPostIds, savedPostIds, commentedPostIds, userCache));
        }

        List<ProjectVO> projects = projectMapper.selectProjectListForReact(currentUserId);
        for (ProjectVO project : projects) {
            items.add(toProjectFeedItem(project, userCache));
            for (MainTaskVO task : mainTaskMapper.selectMainTaskListNonPaging(project.getBizId())) {
                items.add(toTaskFeedItem(task, userCache));
            }
        }

        for (DepartmentScheduleVO schedule : departmentScheduleMapper.selectDepartmentScheduleList()) {
            DashboardFeedItemDTO item = toDepartmentScheduleFeedItem(schedule, userCache);
            if (item != null) {
                items.add(item);
            }
        }

        return items;
    }

    private List<BoardVO> loadAllBoards() {
        Map<String, BoardVO> boards = new LinkedHashMap<>();
        for (BoardVO board : boardMapper.selectNoticeListNonPaging()) {
            boards.put(board.getPstId(), board);
        }
        for (BoardVO board : boardMapper.selectCommunityListNonPaging(null)) {
            boards.putIfAbsent(board.getPstId(), board);
        }
        return new ArrayList<>(boards.values());
    }

    private DashboardFeedItemDTO toBoardFeedItem(
        BoardVO board,
        String currentUserId,
        Set<String> readPostIds,
        Set<String> savedPostIds,
        Set<String> commentedPostIds,
        Map<String, UsersVO> userCache
    ) {
        UsersVO author = readUser(board.getCrtUserId(), userCache);
        BoardCommentVO commentQuery = new BoardCommentVO();
        commentQuery.setPstId(board.getPstId());
        int commentCount = boardCommentMapper.selectBoardCommentTotalCount(commentQuery);
        int viewCount = defaultInt(board.getViewCnt());
        boolean mine = Objects.equals(currentUserId, board.getCrtUserId());

        return DashboardFeedItemDTO.builder()
            .feedId(board.getPstId())
            .itemType("board")
            .categoryCode(board.getBbsCtgrCd())
            .categoryLabel(resolveBoardCategoryLabel(board.getBbsCtgrCd()))
            .createdAt(Objects.requireNonNullElse(board.getLastChgDt(), board.getFrstCrtDt()))
            .actorUserId(board.getCrtUserId())
            .actorName(author == null ? defaultString(board.getUserNm(), board.getCrtUserId()) : defaultString(author.getUserNm(), board.getCrtUserId()))
            .actorFilePath(author == null ? null : author.getFilePath())
            .actorDeptId(author == null ? null : author.getDeptId())
            .actorDeptName(author == null ? board.getDeptNm() : defaultString(author.getDeptNm(), board.getDeptNm()))
            .actorJobGradeName(author == null ? board.getJbgdNm() : defaultString(author.getJbgdNm(), board.getJbgdNm()))
            .title(board.getPstTtl())
            .bodyPreview(stripHtml(board.getContents()))
            .commentCount(commentCount)
            .viewCount(viewCount)
            .reactionScore(viewCount + (commentCount * 10))
            .route("/board")
            .badge(resolveBoardCategoryLabel(board.getBbsCtgrCd()))
            .visibility(NOTICE_CATEGORY.equals(board.getBbsCtgrCd()) ? "company" : "community")
            .read(mine || readPostIds.contains(board.getPstId()))
            .saved(savedPostIds.contains(board.getPstId()))
            .mine(mine)
            .commented(commentedPostIds.contains(board.getPstId()))
            .build();
    }

    private DashboardFeedItemDTO toProjectFeedItem(ProjectVO project, Map<String, UsersVO> userCache) {
        UsersVO manager = readUser(project.getBizPicId(), userCache);
        String preview = defaultString(project.getBizGoal(), project.getBizDetail());
        if (isBlank(preview)) {
            preview = "프로젝트 목표와 진행 현황을 확인하세요.";
        }

        return DashboardFeedItemDTO.builder()
            .feedId("PROJECT-" + project.getBizId())
            .itemType("activity")
            .activityType("project")
            .categoryCode("activity")
            .categoryLabel("업무활동")
            .createdAt(Objects.requireNonNullElse(project.getStrtBizDt(), project.getEndBizDt()))
            .actorUserId(project.getBizPicId())
            .actorName(manager == null ? defaultString(project.getBizPicNm(), project.getBizPicId()) : defaultString(manager.getUserNm(), project.getBizPicId()))
            .actorFilePath(manager == null ? null : manager.getFilePath())
            .actorDeptId(manager == null ? null : manager.getDeptId())
            .actorDeptName(manager == null ? project.getBizPicDeptNm() : defaultString(manager.getDeptNm(), project.getBizPicDeptNm()))
            .actorJobGradeName(manager == null ? project.getBizUserJobNm() : defaultString(manager.getJbgdNm(), project.getBizUserJobNm()))
            .title(project.getBizNm())
            .bodyPreview(preview)
            .commentCount(0)
            .viewCount(defaultInt(project.getBizPrgrs()))
            .reactionScore(0)
            .route("/project")
            .badge(resolveProjectStatusLabel(project.getBizSttsCd()))
            .visibility("team")
            .read(true)
            .saved(false)
            .mine(Objects.equals(project.getBizPicId(), requireCurrentUserId()))
            .commented(false)
            .build();
    }

    private DashboardFeedItemDTO toTaskFeedItem(MainTaskVO task, Map<String, UsersVO> userCache) {
        UsersVO assignee = readUser(task.getBizUserId(), userCache);
        return DashboardFeedItemDTO.builder()
            .feedId("TASK-" + task.getTaskId())
            .itemType("activity")
            .activityType("task")
            .categoryCode("activity")
            .categoryLabel("업무활동")
            .createdAt(Objects.requireNonNullElse(task.getStrtTaskDt(), task.getEndTaskDt()))
            .actorUserId(task.getBizUserId())
            .actorName(assignee == null ? defaultString(task.getBizUserNm(), task.getBizUserId()) : defaultString(assignee.getUserNm(), task.getBizUserId()))
            .actorFilePath(assignee == null ? null : assignee.getFilePath())
            .actorDeptId(assignee == null ? null : assignee.getDeptId())
            .actorDeptName(assignee == null ? task.getBizUserDeptNm() : defaultString(assignee.getDeptNm(), task.getBizUserDeptNm()))
            .actorJobGradeName(assignee == null ? null : assignee.getJbgdNm())
            .title(task.getTaskNm())
            .bodyPreview(buildTaskDescription(task))
            .commentCount(0)
            .viewCount(defaultInt(task.getTaskPrgrs()))
            .reactionScore(0)
            .route("/project")
            .badge(resolveTaskStatusLabel(task.getTaskSttsCd()))
            .visibility("team")
            .read(true)
            .saved(false)
            .mine(Objects.equals(task.getBizUserId(), requireCurrentUserId()))
            .commented(false)
            .build();
    }

    private DashboardFeedItemDTO toDepartmentScheduleFeedItem(DepartmentScheduleVO schedule, Map<String, UsersVO> userCache) {
        if (schedule == null || "Y".equalsIgnoreCase(schedule.getDelYn())) {
            return null;
        }
        if (schedule.getSchdEndDt() != null && schedule.getSchdEndDt().isBefore(LocalDateTime.now().minusDays(7))) {
            return null;
        }

        UsersVO author = readUser(schedule.getDeptSchdCrtUserId(), userCache);
        DepartmentVO department = departmentMapper.selectDepartment(schedule.getDeptId());

        return DashboardFeedItemDTO.builder()
            .feedId("DEPT-" + schedule.getDeptSchdId())
            .itemType("activity")
            .activityType("departmentSchedule")
            .categoryCode("activity")
            .categoryLabel("업무활동")
            .createdAt(Objects.requireNonNullElse(schedule.getSchdStrtDt(), schedule.getSchdEndDt()))
            .actorUserId(schedule.getDeptSchdCrtUserId())
            .actorName(author == null ? schedule.getDeptSchdCrtUserId() : defaultString(author.getUserNm(), schedule.getDeptSchdCrtUserId()))
            .actorFilePath(author == null ? null : author.getFilePath())
            .actorDeptId(schedule.getDeptId())
            .actorDeptName(author != null && !isBlank(author.getDeptNm()) ? author.getDeptNm() : (department == null ? null : department.getDeptNm()))
            .actorJobGradeName(author == null ? null : author.getJbgdNm())
            .title(schedule.getSchdTtl())
            .bodyPreview(defaultString(schedule.getDeptSchdExpln(), buildDateRange(schedule.getSchdStrtDt(), schedule.getSchdEndDt(), schedule.getAllday())))
            .commentCount(0)
            .viewCount(0)
            .reactionScore(0)
            .route("/calendar")
            .badge("부서 일정")
            .visibility("department")
            .read(true)
            .saved(false)
            .mine(Objects.equals(schedule.getDeptSchdCrtUserId(), requireCurrentUserId()))
            .commented(false)
            .build();
    }

    private Map<String, Integer> buildFeedCounts(List<DashboardFeedItemDTO> items) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("all", 0);
        counts.put("unread", 0);
        counts.put("saved", 0);
        counts.put("my-posts", 0);
        counts.put("commented", 0);
        counts.put("activity", 0);
        counts.put(NOTICE_CATEGORY, 0);
        for (String categoryCode : INTEREST_CATEGORY_CODES) {
            counts.put(categoryCode, 0);
        }

        for (DashboardFeedItemDTO item : items) {
            counts.computeIfPresent("all", (key, value) -> value + 1);

            if ("activity".equals(item.getItemType())) {
                counts.computeIfPresent("activity", (key, value) -> value + 1);
                continue;
            }

            if (!Boolean.TRUE.equals(item.getRead()) && !Boolean.TRUE.equals(item.getMine())) {
                counts.computeIfPresent("unread", (key, value) -> value + 1);
            }
            if (Boolean.TRUE.equals(item.getSaved())) {
                counts.computeIfPresent("saved", (key, value) -> value + 1);
            }
            if (Boolean.TRUE.equals(item.getMine())) {
                counts.computeIfPresent("my-posts", (key, value) -> value + 1);
            }
            if (Boolean.TRUE.equals(item.getCommented())) {
                counts.computeIfPresent("commented", (key, value) -> value + 1);
            }
            if (counts.containsKey(item.getCategoryCode())) {
                counts.computeIfPresent(item.getCategoryCode(), (key, value) -> value + 1);
            }
        }
        return counts;
    }

    private List<Map<String, String>> buildDepartmentOptions() {
        return departmentMapper.selectDepartmentList().stream()
            .filter(department -> !"N".equalsIgnoreCase(department.getUseYn()))
            .map(department -> Map.of(
                "deptId", department.getDeptId(),
                "deptNm", defaultString(department.getDeptNm(), department.getDeptId())
            ))
            .toList();
    }

    private List<DashboardWidgetItemDTO> buildTodoItems(UsersVO currentUser) {
        String userId = currentUser.getUserId();
        List<DashboardWidgetItemDTO> items = new ArrayList<>();

        int pendingApprovalCnt = approvalMapper.countMyInboxCombined(Map.of("userId", userId));
        if (pendingApprovalCnt > 0) {
            items.add(DashboardWidgetItemDTO.builder()
                .itemType("approval")
                .title("결재 대기 문서")
                .subtitle("전자결재")
                .description("처리해야 할 문서가 " + pendingApprovalCnt + "건 있습니다.")
                .badge(String.valueOf(pendingApprovalCnt))
                .route("/approval")
                .createdAt(LocalDateTime.now())
                .build());
        }

        dashboardPreferenceMapper.selectTodos(userId).stream()
            .limit(5)
            .forEach(todo -> items.add(DashboardWidgetItemDTO.builder()
                .itemType("todo")
                .title(todo.getTodoTtl())
                .subtitle(isBlank(todo.getTargetUserName()) ? "개인 할 일" : todo.getTargetUserName())
                .description(buildTodoDueText(todo))
                .badge("Y".equalsIgnoreCase(todo.getDoneYn()) ? "완료" : "할 일")
                .route("/")
                .createdAt(todo.getDueDt() == null ? todo.getCrtDt() : todo.getDueDt())
                .build()));

        mainTaskMapper.selectMyTaskListNonPaging(userId).stream()
            .filter(task -> !"B404".equals(task.getTaskSttsCd()))
            .sorted(Comparator.comparing(MainTaskVO::getEndTaskDt, Comparator.nullsLast(Comparator.naturalOrder())))
            .limit(4)
            .forEach(task -> items.add(DashboardWidgetItemDTO.builder()
                .itemType("task")
                .title(task.getTaskNm())
                .subtitle(defaultString(task.getBizNm(), "프로젝트 업무"))
                .description(buildTaskDescription(task))
                .badge(resolveTaskStatusLabel(task.getTaskSttsCd()))
                .route("/project")
                .createdAt(task.getEndTaskDt())
                .build()));

        items.sort(Comparator.comparing(DashboardWidgetItemDTO::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())));
        return items.stream().limit(8).toList();
    }

    private List<DashboardWidgetItemDTO> buildQuickLinks() {
        return List.of(
            DashboardWidgetItemDTO.builder().itemType("quickLink").title("전자결재").description("대기 결재와 기안 문서를 확인합니다.").route("/approval").badge("바로가기").build(),
            DashboardWidgetItemDTO.builder().itemType("quickLink").title("일정").description("개인 일정과 부서 일정을 관리합니다.").route("/calendar").badge("바로가기").build(),
            DashboardWidgetItemDTO.builder().itemType("quickLink").title("프로젝트").description("진행 중인 프로젝트와 업무를 확인합니다.").route("/project").badge("바로가기").build(),
            DashboardWidgetItemDTO.builder().itemType("quickLink").title("게시판").description("공지와 커뮤니티 글을 확인합니다.").route("/board").badge("바로가기").build(),
            DashboardWidgetItemDTO.builder().itemType("quickLink").title("메신저").description("자주 연락하는 사용자와 바로 대화합니다.").route("/messenger").badge("바로가기").build()
        );
    }

    private boolean matchesScope(DashboardFeedItemDTO item, String scope) {
        return switch (scope) {
            case "unread" -> "board".equals(item.getItemType()) && !Boolean.TRUE.equals(item.getRead()) && !Boolean.TRUE.equals(item.getMine());
            case "saved" -> "board".equals(item.getItemType()) && Boolean.TRUE.equals(item.getSaved());
            case "my-posts" -> "board".equals(item.getItemType()) && Boolean.TRUE.equals(item.getMine());
            case "commented" -> "board".equals(item.getItemType()) && Boolean.TRUE.equals(item.getCommented());
            case "activity" -> "activity".equals(item.getItemType());
            default -> true;
        };
    }

    private boolean matchesCategory(DashboardFeedItemDTO item, String category) {
        if ("all".equals(category)) {
            return true;
        }
        if ("activity".equals(category)) {
            return "activity".equals(item.getItemType());
        }
        return Objects.equals(category, item.getCategoryCode());
    }

    private boolean matchesDept(DashboardFeedItemDTO item, String deptId) {
        return isBlank(deptId) || Objects.equals(deptId, item.getActorDeptId());
    }

    private boolean matchesSearch(DashboardFeedItemDTO item, String keyword) {
        if (isBlank(keyword)) {
            return true;
        }
        String lowerKeyword = keyword.toLowerCase(Locale.ROOT);
        return contains(item.getTitle(), lowerKeyword)
            || contains(item.getBodyPreview(), lowerKeyword)
            || contains(item.getActorName(), lowerKeyword)
            || contains(item.getActorDeptName(), lowerKeyword)
            || contains(item.getCategoryLabel(), lowerKeyword)
            || contains(item.getBadge(), lowerKeyword);
    }

    private void sortFeedItems(List<DashboardFeedItemDTO> items, String sort, String scope) {
        if ("reaction".equals(sort) && !"activity".equals(scope)) {
            items.sort((left, right) -> {
                boolean leftBoard = "board".equals(left.getItemType());
                boolean rightBoard = "board".equals(right.getItemType());
                if (leftBoard && rightBoard) {
                    int reactionCompare = Integer.compare(defaultInt(right.getReactionScore()), defaultInt(left.getReactionScore()));
                    if (reactionCompare != 0) {
                        return reactionCompare;
                    }
                    return compareDateDesc(left.getCreatedAt(), right.getCreatedAt());
                }
                if (leftBoard != rightBoard) {
                    return leftBoard ? -1 : 1;
                }
                return compareDateDesc(left.getCreatedAt(), right.getCreatedAt());
            });
            return;
        }

        items.sort((left, right) -> compareDateDesc(left.getCreatedAt(), right.getCreatedAt()));
    }

    private int compareDateDesc(LocalDateTime left, LocalDateTime right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        return right.compareTo(left);
    }

    private boolean isInProgressProject(ProjectVO project) {
        if (project == null) {
            return false;
        }
        return Set.of("B301", "B302", "B303").contains(project.getBizSttsCd());
    }

    private boolean isUpcomingDepartmentSchedule(DepartmentScheduleVO schedule) {
        if (schedule == null || "Y".equalsIgnoreCase(schedule.getDelYn())) {
            return false;
        }
        return !resolveScheduleEnd(schedule.getSchdStrtDt(), schedule.getSchdEndDt()).isBefore(LocalDateTime.now().minusDays(1));
    }

    private boolean isUpcomingUserSchedule(UserScheduleVO schedule) {
        if (schedule == null || "Y".equalsIgnoreCase(schedule.getDelYn())) {
            return false;
        }
        return !resolveScheduleEnd(schedule.getSchdStrtDt(), schedule.getSchdEndDt()).isBefore(LocalDateTime.now().minusDays(1));
    }

    private boolean isScheduleOnDate(LocalDateTime start, LocalDateTime end, String delYn, LocalDate targetDate) {
        if ("Y".equalsIgnoreCase(delYn)) {
            return false;
        }
        LocalDateTime normalizedStart = start == null ? targetDate.atStartOfDay() : start;
        LocalDateTime normalizedEnd = resolveScheduleEnd(start, end);
        return !normalizedEnd.toLocalDate().isBefore(targetDate) && !normalizedStart.toLocalDate().isAfter(targetDate);
    }

    private boolean overlapsWeek(LocalDateTime start, LocalDateTime end, String delYn, LocalDate weekStart, LocalDate weekEnd) {
        if ("Y".equalsIgnoreCase(delYn)) {
            return false;
        }
        LocalDate startDate = start == null ? weekStart : start.toLocalDate();
        LocalDate endDate = resolveScheduleEnd(start, end).toLocalDate();
        return !endDate.isBefore(weekStart) && !startDate.isAfter(weekEnd);
    }

    private LocalDateTime resolveScheduleEnd(LocalDateTime start, LocalDateTime end) {
        if (end != null) {
            return end;
        }
        return start == null ? LocalDateTime.now() : start;
    }

    private String resolveBoardCategoryLabel(String categoryCode) {
        return BOARD_CATEGORY_LABELS.getOrDefault(categoryCode, "커뮤니티");
    }

    private String resolveProjectStatusLabel(String statusCode) {
        return PROJECT_STATUS_LABELS.getOrDefault(statusCode, "프로젝트");
    }

    private String resolveTaskStatusLabel(String statusCode) {
        return TASK_STATUS_LABELS.getOrDefault(statusCode, "업무");
    }

    private String normalizeScope(String scope) {
        return scope != null && FEED_SCOPES.contains(scope) ? scope : "all";
    }

    private String normalizeCategory(String category) {
        if ("activity".equals(category) || (category != null && BOARD_CATEGORY_CODES.contains(category))) {
            return category;
        }
        return "all";
    }

    private String normalizeSort(String sort) {
        return "reaction".equals(sort) ? "reaction" : "recent";
    }

    private String normalizeView(String view) {
        return "expanded".equals(view) ? "expanded" : "summary";
    }

    private String normalizeAccepted(String acceptedYn, String fallback) {
        if ("Y".equalsIgnoreCase(acceptedYn)) {
            return "Y";
        }
        if ("N".equalsIgnoreCase(acceptedYn)) {
            return "N";
        }
        return normalizeYn(fallback, "N");
    }

    private String normalizeYn(String value, String fallback) {
        if ("Y".equalsIgnoreCase(value)) {
            return "Y";
        }
        if ("N".equalsIgnoreCase(value)) {
            return "N";
        }
        return "Y".equalsIgnoreCase(fallback) ? "Y" : "N";
    }

    private String normalizeRecommendationBox(String box) {
        return box != null && Set.of("inbox", "sent", "all").contains(box) ? box : "inbox";
    }

    private int unreadRecommendationCount(String userId) {
        return (int) dashboardPreferenceMapper.selectReceivedRecommendations(userId).stream()
            .filter(item -> !"Y".equalsIgnoreCase(item.getReadYn()))
            .count();
    }

    private List<String> sanitizeInterestCategories(Collection<String> categoryCodes) {
        if (categoryCodes == null) {
            return List.of();
        }
        return categoryCodes.stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(INTEREST_CATEGORY_CODES::contains)
            .distinct()
            .sorted()
            .toList();
    }

    private void hydrateRecommendationLabel(DashboardCategoryRecommendationDTO recommendation) {
        if (recommendation == null) {
            return;
        }
        recommendation.setCategoryLabel(resolveBoardCategoryLabel(recommendation.getBbsCtgrCd()));
    }

    private DashboardTodoDTO findTodoOrThrow(Long todoId) {
        return dashboardPreferenceMapper.selectTodos(requireCurrentUserId()).stream()
            .filter(todo -> Objects.equals(todo.getTodoId(), todoId))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "할 일을 찾을 수 없습니다."));
    }

    private UsersVO ensureUserExists(String userId) {
        UsersVO user = userMapper.selectUser(userId);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.");
        }
        return user;
    }

    private BoardVO ensureBoardExists(String pstId) {
        BoardVO board = boardMapper.selectBoard(pstId);
        if (board == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다.");
        }
        return board;
    }

    private UsersVO requireCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new IllegalStateException("No authenticated user");
        }
        if (authentication.getPrincipal() instanceof CustomUserDetails details) {
            return details.getRealUser();
        }
        return ensureUserExists(authentication.getName());
    }

    private String requireCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new IllegalStateException("No authenticated user");
        }
        return authentication.getName();
    }

    private UsersVO readUser(String userId, Map<String, UsersVO> userCache) {
        if (isBlank(userId)) {
            return null;
        }
        return userCache.computeIfAbsent(userId, userMapper::selectUser);
    }

    private String buildTaskDescription(MainTaskVO task) {
        String dueText = task.getEndTaskDt() == null ? "마감일 미정" : "마감 " + task.getEndTaskDt().toLocalDate();
        String assignee = defaultString(task.getBizUserNm(), task.getBizUserId());
        return assignee + " · " + dueText;
    }

    private String buildTodoDueText(DashboardTodoDTO todo) {
        String due = todo.getDueDt() == null ? "기한 없음" : "기한 " + todo.getDueDt().toLocalDate();
        if (isBlank(todo.getTodoCn())) {
            return due;
        }
        return due + " · " + todo.getTodoCn();
    }

    private String buildDateRange(LocalDateTime start, LocalDateTime end, String allday) {
        if (start == null && end == null) {
            return "일정 시간 미정";
        }
        if ("Y".equalsIgnoreCase(allday)) {
            if (start == null || end == null) {
                return "종일 일정";
            }
            return start.toLocalDate() + " - " + end.toLocalDate();
        }
        if (start == null) {
            return String.valueOf(end);
        }
        if (end == null) {
            return String.valueOf(start);
        }
        return start + " - " + end;
    }

    private String formatTimeOnly(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return value.toLocalTime().toString();
    }

    private String stripHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
            .replaceAll("<[^>]*>", " ")
            .replace("&nbsp;", " ")
            .replaceAll("\\s+", " ")
            .trim();
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private String requireText(String value, String message) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String defaultString(String primary, String fallback) {
        return isBlank(primary) ? fallback : primary;
    }
}
