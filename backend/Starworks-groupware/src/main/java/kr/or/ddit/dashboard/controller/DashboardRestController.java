package kr.or.ddit.dashboard.controller;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import kr.or.ddit.dashboard.dto.DashboardFeedPreferenceDTO;
import kr.or.ddit.dashboard.dto.DashboardTodoDTO;
import kr.or.ddit.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/rest/dashboard")
@RequiredArgsConstructor
public class DashboardRestController {

    private final DashboardService dashboardService;

    @GetMapping
    public Map<String, Object> readDashboard() {
        return Map.of(
            "attendanceRecord", dashboardService.getAttendanceRecord(),
            "counts", dashboardService.getCntCard(),
            "todaySchedules", dashboardService.getTodayScheDuleList(),
            "currentProjects", dashboardService.getCurrentProjectList(),
            "recentBoards", dashboardService.getRecentBoardList(),
            "departmentStatus", dashboardService.getDepartStatusCnt()
        );
    }

    @GetMapping("/feed")
    public Map<String, Object> readFeed(
        @RequestParam(defaultValue = "all") String scope,
        @RequestParam(defaultValue = "all") String category,
        @RequestParam(required = false) String deptId,
        @RequestParam(required = false) String q,
        @RequestParam(defaultValue = "recent") String sort,
        @RequestParam(defaultValue = "summary") String view,
        @RequestParam(defaultValue = "1") int page
    ) {
        return dashboardService.getFeed(scope, category, deptId, q, sort, view, page);
    }

    @GetMapping("/widgets")
    public Map<String, Object> readWidgets() {
        return dashboardService.getWidgets();
    }

    @GetMapping("/bootstrap")
    public Map<String, Object> readBootstrap() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("preferences", dashboardService.getFeedPreference());
        response.put("categories", dashboardService.getFavoriteCategories());
        response.put("widgets", dashboardService.getWidgets());
        response.put("todos", dashboardService.getTodos());
        response.put("recommendations", dashboardService.getCategoryRecommendations("inbox"));
        return response;
    }

    @GetMapping("/preferences")
    public DashboardFeedPreferenceDTO readPreferences() {
        return dashboardService.getFeedPreference();
    }

    @PutMapping("/preferences")
    public DashboardFeedPreferenceDTO savePreferences(@RequestBody DashboardFeedPreferenceDTO preference) {
        return dashboardService.saveFeedPreference(preference);
    }

    @GetMapping("/categories")
    public Map<String, Object> readFavoriteCategories() {
        return Map.of("categories", dashboardService.getFavoriteCategories());
    }

    @PutMapping("/categories")
    public Map<String, Object> saveFavoriteCategories(@RequestBody Map<String, List<String>> body) {
        return Map.of("categories", dashboardService.saveFavoriteCategories(body.get("categories")));
    }

    @PostMapping("/board-read/{pstId}")
    public Map<String, Object> markBoardRead(@PathVariable String pstId) {
        dashboardService.markBoardRead(pstId);
        return Map.of("success", true);
    }

    @PostMapping("/saved-posts/{pstId}")
    public Map<String, Object> savePost(@PathVariable String pstId) {
        dashboardService.savePost(pstId);
        return Map.of("success", true);
    }

    @DeleteMapping("/saved-posts/{pstId}")
    public Map<String, Object> deleteSavedPost(@PathVariable String pstId) {
        dashboardService.unsavePost(pstId);
        return Map.of("success", true);
    }

    @GetMapping("/favorite-users")
    public List<?> readFavoriteUsers() {
        return dashboardService.getFavoriteUsers();
    }

    @PostMapping("/favorite-users/{targetUserId}")
    public Map<String, Object> createFavoriteUser(@PathVariable String targetUserId) {
        dashboardService.addFavoriteUser(targetUserId);
        return Map.of("success", true);
    }

    @DeleteMapping("/favorite-users/{targetUserId}")
    public Map<String, Object> deleteFavoriteUser(@PathVariable String targetUserId) {
        dashboardService.removeFavoriteUser(targetUserId);
        return Map.of("success", true);
    }

    @GetMapping("/todos")
    public List<DashboardTodoDTO> readTodos() {
        return dashboardService.getTodos();
    }

    @PostMapping("/todos")
    public DashboardTodoDTO createTodo(@RequestBody DashboardTodoDTO todo) {
        return dashboardService.createTodo(todo);
    }

    @PatchMapping("/todos/{todoId}")
    public DashboardTodoDTO updateTodo(@PathVariable Long todoId, @RequestBody DashboardTodoDTO todo) {
        return dashboardService.updateTodo(todoId, todo);
    }

    @DeleteMapping("/todos/{todoId}")
    public Map<String, Object> deleteTodo(@PathVariable Long todoId) {
        dashboardService.deleteTodo(todoId);
        return Map.of("success", true);
    }

    @GetMapping("/category-recommendations")
    public Map<String, Object> readCategoryRecommendations(@RequestParam(defaultValue = "inbox") String box) {
        return dashboardService.getCategoryRecommendations(box);
    }

    @PostMapping("/category-recommendations")
    public Map<String, Object> createCategoryRecommendations(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> categoryCodes = (List<String>) body.get("categoryCodes");
        return Map.of(
            "items",
            dashboardService.createCategoryRecommendations(
                String.valueOf(body.get("targetUserId")),
                categoryCodes,
                body.get("message") == null ? null : String.valueOf(body.get("message"))
            )
        );
    }

    @PatchMapping("/category-recommendations/{recommendId}")
    public Object updateCategoryRecommendation(@PathVariable Long recommendId, @RequestBody Map<String, String> body) {
        return dashboardService.updateCategoryRecommendation(recommendId, body.get("acceptedYn"), body.get("readYn"));
    }

    @GetMapping("/profile/{userId}")
    public Object readProfile(@PathVariable String userId) {
        return dashboardService.getProfile(userId);
    }
}
