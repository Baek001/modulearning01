package kr.or.ddit.vo;

import java.util.Date;

import lombok.Data;

@Data
public class CommunityMemberVO {
    private Long communityId;
    private String userId;
    private String roleCd;
    private String statusCd;
    private Date crtDt;
    private Date lastChgDt;
    private String userNm;
    private String deptNm;
    private String jbgdNm;
    private String filePath;
    private String userEmail;
    private String inviteMessage;
}
