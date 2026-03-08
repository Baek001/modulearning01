package kr.or.ddit.vo;

import java.util.Date;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.Include;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class MessengerRoomVO {

    @Include
    @NotBlank
    private String msgrId;

    @NotBlank
    private String msgrNm;
    private Date crtDt;
    private String delYn;
    private String roomTypeCd;
    private String pinnedMsgContId;
    private String pinnedBy;
    private Date pinnedDt;
    private String currentUserRole;
    private String notifyYn;
    private String ownerUserId;
    private String pinnedMessagePreview;

    private String partnerFilePath;
    private String partnerDeptNm;
    private String partnerJbgdNm;

    private String lastMsgCont;
    private Date lastMsgDt;

    private int unreadCount;
    private Integer memberCount;
    private int participantCount;
}
