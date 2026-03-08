package kr.or.ddit.dashboard.service;

import java.util.List;
import java.util.Map;

import kr.or.ddit.dashboard.dto.CntCardDTO;
import kr.or.ddit.dashboard.dto.DashboardCategoryRecommendationDTO;
import kr.or.ddit.dashboard.dto.DashboardFavoriteUserDTO;
import kr.or.ddit.dashboard.dto.DashboardFeedPreferenceDTO;
import kr.or.ddit.dashboard.dto.DashboardProfileDTO;
import kr.or.ddit.dashboard.dto.DashboardTodoDTO;
import kr.or.ddit.dashboard.dto.CurrentProjectDTO;
import kr.or.ddit.dashboard.dto.DepartStatusDTO;
import kr.or.ddit.dashboard.dto.RecentBoardDTO;
import kr.or.ddit.dashboard.dto.TodayScheduleDTO;

public interface DashboardService {

	Map<String, Object> getAttendanceRecord();

	CntCardDTO getCntCard();

	List<TodayScheduleDTO> getTodayScheDuleList();

	List<CurrentProjectDTO> getCurrentProjectList();

	List<RecentBoardDTO> getRecentBoardList();

	DepartStatusDTO getDepartStatusCnt();

	Map<String, Object> getFeed(String scope, String category, String deptId, String q, String sort, String view, int page);

	Map<String, Object> getWidgets();

	DashboardFeedPreferenceDTO getFeedPreference();

	DashboardFeedPreferenceDTO saveFeedPreference(DashboardFeedPreferenceDTO preference);

	List<String> getFavoriteCategories();

	List<String> saveFavoriteCategories(List<String> categoryCodes);

	void markBoardRead(String pstId);

	void savePost(String pstId);

	void unsavePost(String pstId);

	List<DashboardFavoriteUserDTO> getFavoriteUsers();

	void addFavoriteUser(String targetUserId);

	void removeFavoriteUser(String targetUserId);

	List<DashboardTodoDTO> getTodos();

	DashboardTodoDTO createTodo(DashboardTodoDTO todo);

	DashboardTodoDTO updateTodo(Long todoId, DashboardTodoDTO todo);

	void deleteTodo(Long todoId);

	Map<String, Object> getCategoryRecommendations(String box);

	List<DashboardCategoryRecommendationDTO> createCategoryRecommendations(String targetUserId, List<String> categoryCodes, String message);

	DashboardCategoryRecommendationDTO updateCategoryRecommendation(Long recommendId, String acceptedYn, String readYn);

	DashboardProfileDTO getProfile(String userId);
}
