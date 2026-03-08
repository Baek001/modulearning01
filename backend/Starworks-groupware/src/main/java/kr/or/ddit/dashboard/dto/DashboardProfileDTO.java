package kr.or.ddit.dashboard.dto;

import java.util.List;

import kr.or.ddit.vo.BoardVO;
import kr.or.ddit.vo.UserHistoryVO;
import kr.or.ddit.vo.UsersVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardProfileDTO {

    private UsersVO user;
    private boolean favorite;
    private List<BoardVO> recentBoards;
    private List<DashboardFeedItemDTO> recentActivities;
    private List<UserHistoryVO> histories;
}
