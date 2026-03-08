package kr.or.ddit.vo;

import java.util.Date;
import java.util.List;

import lombok.Data;

@Data
public class ChatMessageDTO {
    private String msgContId;
    private String msgrId;
    private String userId;
    private String userNm;
    private String contents;
    private Date sendDt;
    private MessageType type;
    private String userImgFileId;
    private int unreadCount;
    private String userFilePath;
    private String jbgdNm;
    private String deptNm;
    private String msgTypeCd;
    private String forwardFromMsgContId;
    private String forwardPreview;
    private List<FileDetailVO> attachments;
    private String targetUserId;

    public enum MessageType {
        CHAT,
        JOIN,
        LEAVE,
        FILE,
        DELETE,
        PIN,
        ROOM_UPDATE
    }

    public ChatMessageDTO() {
        this.sendDt = new Date();
        this.type = MessageType.CHAT;
    }
}
