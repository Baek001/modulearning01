package kr.or.ddit.vo;

import java.util.Date;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.NotBlank;
import kr.or.ddit.comm.file.FileAttachable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.Include;
import lombok.ToString;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class MessengerContentVO implements FileAttachable {

    @Include
    @NotBlank
    private String msgContId;

    @NotBlank
    private String userId;

    @NotBlank
    private String msgrId;
    private String readYn;
    private String contents;
    private Date sendDt;
    private String delYn;
    private String msgFileId;
    private String msgTypeCd;
    private String forwardFromMsgContId;
    private String forwardPreview;
    private String forwardUserNm;
    private List<FileDetailVO> attachments;

    private String userNm;
    private int unreadCount;
    private String userImgFileId;
    private String userFilePath;
    private String jbgdNm;
    private String deptNm;

    @JsonIgnore
    @ToString.Exclude
    private List<MultipartFile> fileList;

    @Override
    public List<MultipartFile> getFileList() {
        return this.fileList;
    }

    @Override
    public void setFileId(String fileId) {
        this.msgFileId = fileId;
    }
}
