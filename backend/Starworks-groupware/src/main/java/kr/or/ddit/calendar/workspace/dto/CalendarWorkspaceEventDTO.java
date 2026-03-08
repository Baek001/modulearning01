package kr.or.ddit.calendar.workspace.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class CalendarWorkspaceEventDTO {

    private String eventKey;
    private String sourceCd;
    private String sourceGroupCd;
    private String title;
    private LocalDateTime startDt;
    private LocalDateTime endDt;
    private String alldayYn;
    private String description;
    private String ownerUserId;
    private String ownerUserNm;
    private String deptId;
    private String deptNm;
    private Long communityId;
    private String communityNm;
    private String pstId;
    private String bizId;
    private String projectNm;
    private Long todoId;
    private String colorCd;
    private String sourceLabel;
    private String typeLabel;
    private boolean canEdit;
    private boolean canDelete;
    private String detailHref;
    private String placeText;
    private String placeUrl;
    private String repeatRule;
    private String doneYn;
    private String statusLabel;
}
