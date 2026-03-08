package kr.or.ddit.vo;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.NotBlank;
import kr.or.ddit.comm.file.FileAttachable;
import kr.or.ddit.comm.validate.InsertGroupNotDefault;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.Include;
import lombok.ToString;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class BoardCommentVO implements FileAttachable {

    @NotBlank(groups = InsertGroupNotDefault.class)
    private String pstId;

    @Include
    private String cmntSqn;

    private String upCmntSqn;

    @NotBlank(groups = InsertGroupNotDefault.class)
    private String contents;

    private String delYn;

    @NotBlank
    private String crtUserId;

    private LocalDateTime frstCrtDt;
    private LocalDateTime lastChgDt;
    private String lastChgUserId;
    private String cmntFileId;

    private UsersVO users;
    private String filePath;
    private List<FileDetailVO> attachments;
    private List<String> mentionUserIds;

    @JsonIgnore
    @ToString.Exclude
    private List<MultipartFile> fileList;

    @Override
    public List<MultipartFile> getFileList() {
        return fileList;
    }

    @Override
    public void setFileId(String fileId) {
        this.cmntFileId = fileId;
    }

    public Date getFrstCrtDtAsUtilDate() {
        if (frstCrtDt == null) {
            return null;
        }
        return Date.from(frstCrtDt.atZone(ZoneId.systemDefault()).toInstant());
    }
}
