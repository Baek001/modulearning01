package kr.or.ddit.vo;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.NotBlank;
import kr.or.ddit.comm.file.FileAttachable;
import kr.or.ddit.comm.validate.UpdateGroup;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.Include;
import lombok.ToString;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class BoardVO implements FileAttachable {

    @Include
    @NotBlank(groups = UpdateGroup.class)
    private String pstId;

    @NotBlank
    private String bbsCtgrCd;

    @NotBlank
    private String pstTtl;

    @NotBlank
    private String contents;

    private Integer viewCnt;

    @NotBlank(groups = UpdateGroup.class)
    private String crtUserId;

    private String delYn;
    private LocalDateTime frstCrtDt;
    private LocalDateTime lastChgDt;
    private String lastChgUserId;
    private String pstFileId;
    private String fixedYn;

    private Long communityId;
    private String communityNm;
    private String communityTypeCd;
    private String communityAuthorRoleCd;
    private String visibilityCd;
    private String pstTypeCd;
    private String importanceCd;
    private String linkUrl;
    private String publishStateCd;
    private LocalDateTime reservedPublishDt;
    private LocalDateTime publishedDt;

    private Integer likeCount;
    private Integer commentCount;
    private Integer readerCount;
    private Integer shareCount;
    private Boolean saved;
    private Boolean liked;
    private Boolean manageable;
    private Boolean pinnable;
    private Boolean shareable;

    private UsersVO users;

    private String userNm;
    private String jbgdNm;
    private String deptNm;

    private List<FileDetailVO> attachments;
    private List<BoardPollOptionVO> pollOptions;
    private List<BoardCommentVO> comments;
    private List<BoardReactionVO> likes;
    private List<BoardShareRecipientVO> shareRecipients;
    private List<BoardMentionVO> mentions;

    private BoardPollVO poll;
    private BoardScheduleVO schedule;
    private BoardTodoVO todo;

    private List<String> mentionUserIds;
    private Boolean mentionAll;

    @JsonIgnore
    @ToString.Exclude
    private List<MultipartFile> fileList;

    @Override
    public List<MultipartFile> getFileList() {
        return this.fileList;
    }

    @Override
    public void setFileId(String fileId) {
        this.pstFileId = fileId;
    }

    public Date getFrstCrtDtAsUtilDate() {
        if (frstCrtDt == null) {
            return null;
        }
        return Date.from(frstCrtDt.atZone(ZoneId.systemDefault()).toInstant());
    }
}
