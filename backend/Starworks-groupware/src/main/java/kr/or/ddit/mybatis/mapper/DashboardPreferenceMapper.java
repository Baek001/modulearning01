package kr.or.ddit.mybatis.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import kr.or.ddit.dashboard.dto.DashboardCategoryRecommendationDTO;
import kr.or.ddit.dashboard.dto.DashboardFavoriteUserDTO;
import kr.or.ddit.dashboard.dto.DashboardFeedPreferenceDTO;
import kr.or.ddit.dashboard.dto.DashboardTodoDTO;

@Mapper
public interface DashboardPreferenceMapper {

    DashboardFeedPreferenceDTO selectPreference(@Param("userId") String userId);

    int upsertPreference(DashboardFeedPreferenceDTO preference);

    List<String> selectCategoryCodes(@Param("userId") String userId);

    int deleteCategoryCodes(@Param("userId") String userId);

    int insertCategoryCode(@Param("userId") String userId, @Param("bbsCtgrCd") String bbsCtgrCd);

    List<String> selectReadPostIds(@Param("userId") String userId);

    int insertReadPost(@Param("userId") String userId, @Param("pstId") String pstId);

    List<String> selectSavedPostIds(@Param("userId") String userId);

    int insertSavedPost(@Param("userId") String userId, @Param("pstId") String pstId);

    int deleteSavedPost(@Param("userId") String userId, @Param("pstId") String pstId);

    List<String> selectCommentedPostIds(@Param("userId") String userId);

    List<DashboardFavoriteUserDTO> selectFavoriteUsers(@Param("userId") String userId);

    int insertFavoriteUser(@Param("userId") String userId, @Param("targetUserId") String targetUserId);

    int deleteFavoriteUser(@Param("userId") String userId, @Param("targetUserId") String targetUserId);

    int countFavoriteUser(@Param("userId") String userId, @Param("targetUserId") String targetUserId);

    List<DashboardTodoDTO> selectTodos(@Param("userId") String userId);

    int insertTodo(DashboardTodoDTO todo);

    int updateTodo(DashboardTodoDTO todo);

    int deleteTodo(@Param("userId") String userId, @Param("todoId") Long todoId);

    DashboardCategoryRecommendationDTO selectRecommendation(@Param("recommendId") Long recommendId);

    List<DashboardCategoryRecommendationDTO> selectReceivedRecommendations(@Param("userId") String userId);

    List<DashboardCategoryRecommendationDTO> selectSentRecommendations(@Param("userId") String userId);

    int insertCategoryRecommendation(DashboardCategoryRecommendationDTO recommendation);

    int updateCategoryRecommendation(DashboardCategoryRecommendationDTO recommendation);
}
