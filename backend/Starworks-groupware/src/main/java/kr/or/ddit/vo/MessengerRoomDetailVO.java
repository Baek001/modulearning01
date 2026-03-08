package kr.or.ddit.vo;

import java.util.List;

import lombok.Data;

@Data
public class MessengerRoomDetailVO {
    private MessengerRoomVO room;
    private MessengerContentVO pinnedMessage;
    private List<MessengerParticipantVO> participants;
    private String currentUserRole;
    private String ownerUserId;
    private boolean notifyEnabled;
}
