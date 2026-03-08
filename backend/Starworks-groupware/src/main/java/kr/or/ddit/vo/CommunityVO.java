package kr.or.ddit.vo;

import java.util.List;
import java.util.Date;

import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.annotation.JsonIgnore;

import kr.or.ddit.comm.file.FileAttachable;
import lombok.Data;
import lombok.ToString;

@Data
public class CommunityVO implements FileAttachable {
    private Long communityId;
    private String communityNm;
    private String communityDesc;
    private String communityTypeCd;
    private String visibilityCd;
    private String joinPolicyCd;
    private String deptId;
    private String deptNm;
    private String introText;
    private String iconFileId;
    private String coverFileId;
    private String iconFilePath;
    private String coverFilePath;
    private String postTemplateHtml;
    private String closedYn;
    private Date closedDt;
    private String ownerUserId;
    private String ownerUserNm;
    private String delYn;
    private Date crtDt;
    private Date lastChgDt;
    private int memberCount;
    private int operatorCount;
    private int pendingCount;
    private String memberRoleCd;
    private String memberStatusCd;
    private String joinedYn;
    private String favoriteYn;
    private Integer sortOrdr;
    private Boolean manageable;
    private Boolean joinable;
    private Boolean leavable;
    private Boolean closable;
    private Boolean joined;

    @JsonIgnore
    @ToString.Exclude
    private List<MultipartFile> fileList;

    @Override
    public List<MultipartFile> getFileList() {
        return fileList;
    }

    @Override
    public void setFileId(String fileId) {
        this.iconFileId = fileId;
    }
}
