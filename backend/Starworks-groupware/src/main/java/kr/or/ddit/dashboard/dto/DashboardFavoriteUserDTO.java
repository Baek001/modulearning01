package kr.or.ddit.dashboard.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardFavoriteUserDTO {

    private String userId;
    private String targetUserId;
    private String userNm;
    private String userEmail;
    private String userTelno;
    private String extTel;
    private String deptId;
    private String deptNm;
    private String jbgdCd;
    private String jbgdNm;
    private String workSttsCd;
    private String workSttsNm;
    private String filePath;
    private LocalDateTime crtDt;
}
