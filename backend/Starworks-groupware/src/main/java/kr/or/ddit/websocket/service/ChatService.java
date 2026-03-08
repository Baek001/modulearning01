package kr.or.ddit.websocket.service;

import java.util.List;

import kr.or.ddit.vo.MessengerContentVO;
import kr.or.ddit.vo.MessengerPanelVO;
import kr.or.ddit.vo.MessengerParticipantVO;
import kr.or.ddit.vo.MessengerRoomDetailVO;
import kr.or.ddit.vo.MessengerRoomVO;
import kr.or.ddit.vo.UsersVO;

public interface ChatService {

    UsersVO getCurrentUser(String userId);

    List<UsersVO> getAvailableUsers(String userId);

    List<MessengerParticipantVO> getRoomParticipants(String msgrId, String userId);

    int getRoomParticipantCount(String msgrId);

    void updateRoomName(String msgrId, String msgrNm, String requesterUserId);

    MessengerRoomVO createGroupRoom(List<String> userIds, String msgrNm, String creatorUserId, String roomTypeCd);

    void inviteUsers(String msgrId, List<String> userIds, String requesterUserId);

    void kickUser(String msgrId, String targetUserId, String requesterUserId);

    void markAllAsRead(String msgrId, String userId);

    List<MessengerRoomVO> getMyRooms(String userId, String scope, String keyword, String type);

    MessengerPanelVO getPanel(String userId);

    MessengerRoomDetailVO getRoomDetail(String msgrId, String userId);

    List<MessengerContentVO> getRoomMessages(String msgrId, String userId);

    List<MessengerContentVO> searchRoomMessages(String msgrId, String userId, String keyword);

    List<MessengerContentVO> getRoomMessagesForExport(String msgrId, String userId);

    MessengerRoomVO findOrCreatePrivateRoom(String currentUserId, String targetUserId);

    MessengerRoomVO findOrCreateSelfRoom(String userId);

    MessengerContentVO saveMessage(MessengerContentVO message);

    MessengerContentVO saveFileMessage(String msgrId, String userId, String contents, List<org.springframework.web.multipart.MultipartFile> files);

    void updateLeftTime(String msgrId, String userId);

    void markAsRead(String msgContId);

    MessengerContentVO deleteMessage(String msgrId, String msgContId, String userId);

    MessengerContentVO forwardMessage(String msgContId, String targetRoomId, String userId);

    MessengerContentVO pinMessage(String msgrId, String msgContId, String userId);

    void clearPinnedMessage(String msgrId, String userId);

    void updateNotify(String msgrId, String userId, boolean notifyEnabled);
}
