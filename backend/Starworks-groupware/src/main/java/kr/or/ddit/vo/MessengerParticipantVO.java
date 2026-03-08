package kr.or.ddit.vo;

import lombok.Data;

@Data
public class MessengerParticipantVO {
    private String userId;
    private String userNm;
    private String deptNm;
    private String jbgdNm;
    private String filePath;
    private String roleCd;
    private String notifyYn;
    private boolean me;
}
