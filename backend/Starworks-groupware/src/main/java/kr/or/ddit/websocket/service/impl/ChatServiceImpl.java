package kr.or.ddit.websocket.service.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import kr.or.ddit.comm.file.FileFolderType;
import kr.or.ddit.comm.file.service.FileDetailService;
import kr.or.ddit.comm.file.service.impl.FileUploadServiceImpl;
import kr.or.ddit.mybatis.mapper.MessengerContentMapper;
import kr.or.ddit.mybatis.mapper.MessengerReadMapper;
import kr.or.ddit.mybatis.mapper.MessengerRoomMapper;
import kr.or.ddit.mybatis.mapper.MessengerUserMapper;
import kr.or.ddit.mybatis.mapper.UsersMapper;
import kr.or.ddit.vo.MessengerContentVO;
import kr.or.ddit.vo.MessengerPanelVO;
import kr.or.ddit.vo.MessengerParticipantVO;
import kr.or.ddit.vo.MessengerRoomDetailVO;
import kr.or.ddit.vo.MessengerRoomVO;
import kr.or.ddit.vo.MessengerUserVO;
import kr.or.ddit.vo.UsersVO;
import kr.or.ddit.websocket.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final MessengerRoomMapper roomMapper;
    private final MessengerUserMapper userMapper;
    private final MessengerContentMapper contentMapper;
    private final UsersMapper usersMapper;
    private final MessengerReadMapper readMapper;
    private final FileUploadServiceImpl fileUploadService;
    private final FileDetailService fileDetailService;

    @Override
    public UsersVO getCurrentUser(String userId) {
        return usersMapper.selectUser(userId);
    }

    @Override
    public List<UsersVO> getAvailableUsers(String userId) {
        return usersMapper.selectUserList().stream()
            .filter(user -> !Objects.equals(user.getUserId(), userId))
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MessengerRoomVO createGroupRoom(List<String> userIds, String msgrNm, String creatorUserId, String roomTypeCd) {
        MessengerRoomVO roomVO = new MessengerRoomVO();
        roomVO.setMsgrNm(StringUtils.defaultIfBlank(msgrNm, defaultRoomName(roomTypeCd)));
        roomVO.setRoomTypeCd(StringUtils.defaultIfBlank(roomTypeCd, "group"));

        int result = roomMapper.insertMessengerRoom(roomVO);
        if (result != 1 || roomVO.getMsgrId() == null) {
            throw new IllegalStateException("채팅방 생성에 실패했습니다.");
        }

        List<String> normalizedUserIds = normalizeUserIds(userIds, creatorUserId);
        userMapper.upsertRoomMember(roomVO.getMsgrId(), creatorUserId, "owner");
        for (String userId : normalizedUserIds) {
            if (!Objects.equals(userId, creatorUserId)) {
                userMapper.upsertRoomMember(roomVO.getMsgrId(), userId, "member");
            }
        }

        saveSystemMessageInternal(roomVO.getMsgrId(), creatorUserId, "대화방이 생성되었습니다.");
        return roomMapper.selectMyRoom(roomVO.getMsgrId(), creatorUserId);
    }

    @Override
    public List<MessengerRoomVO> getMyRooms(String userId, String scope, String keyword, String type) {
        return roomMapper.selectMyRooms(userId).stream()
            .filter(room -> filterByScope(room, scope))
            .filter(room -> filterByType(room, type))
            .filter(room -> filterByKeyword(room, keyword))
            .sorted(Comparator
                .comparing(MessengerRoomVO::getLastMsgDt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(MessengerRoomVO::getCrtDt, Comparator.nullsLast(Comparator.reverseOrder())))
            .collect(Collectors.toList());
    }

    @Override
    public MessengerPanelVO getPanel(String userId) {
        List<MessengerRoomVO> rooms = getMyRooms(userId, "all", null, null);
        MessengerPanelVO panel = new MessengerPanelVO();
        List<MessengerRoomVO> notifyRooms = rooms.stream()
            .filter(room -> !"N".equalsIgnoreCase(room.getNotifyYn()))
            .collect(Collectors.toList());
        panel.setRooms(notifyRooms.stream().limit(8).collect(Collectors.toList()));
        panel.setUnreadRoomCount((int) notifyRooms.stream().filter(room -> room.getUnreadCount() > 0).count());
        panel.setUnreadMessageCount(notifyRooms.stream().mapToInt(MessengerRoomVO::getUnreadCount).sum());
        return panel;
    }

    @Override
    public MessengerRoomDetailVO getRoomDetail(String msgrId, String userId) {
        MessengerRoomVO room = requireRoom(msgrId, userId);
        MessengerRoomDetailVO detail = new MessengerRoomDetailVO();
        detail.setRoom(room);
        detail.setParticipants(getRoomParticipants(msgrId, userId));
        detail.setCurrentUserRole(room.getCurrentUserRole());
        detail.setOwnerUserId(room.getOwnerUserId());
        detail.setNotifyEnabled(!"N".equalsIgnoreCase(room.getNotifyYn()));
        if (StringUtils.isNotBlank(room.getPinnedMsgContId())) {
            detail.setPinnedMessage(enrichMessage(contentMapper.selectMessengerContent(room.getPinnedMsgContId(), userId)));
        }
        return detail;
    }

    @Override
    public List<MessengerContentVO> getRoomMessages(String msgrId, String userId) {
        requireRoomMembership(msgrId, userId);
        return enrichMessages(contentMapper.selectMessengerContentByRoomId(msgrId, userId));
    }

    @Override
    public List<MessengerContentVO> searchRoomMessages(String msgrId, String userId, String keyword) {
        requireRoomMembership(msgrId, userId);
        if (StringUtils.isBlank(keyword)) {
            return List.of();
        }
        return enrichMessages(contentMapper.searchMessengerContentByRoomId(msgrId, userId, keyword.trim()));
    }

    @Override
    public List<MessengerContentVO> getRoomMessagesForExport(String msgrId, String userId) {
        requireRoomMembership(msgrId, userId);
        return enrichMessages(contentMapper.selectMessengerContentForExport(msgrId, userId));
    }

    @Override
    @Transactional
    public MessengerRoomVO findOrCreatePrivateRoom(String currentUserId, String targetUserId) {
        if (Objects.equals(currentUserId, targetUserId)) {
            return findOrCreateSelfRoom(currentUserId);
        }

        MessengerRoomVO existingRoom = roomMapper.findPrivateRoom(currentUserId, targetUserId);
        if (existingRoom != null) {
            return roomMapper.selectMyRoom(existingRoom.getMsgrId(), currentUserId);
        }

        MessengerRoomVO roomVO = new MessengerRoomVO();
        roomVO.setRoomTypeCd("private");
        roomVO.setMsgrNm(findUserNmByUserId(targetUserId));
        roomMapper.insertMessengerRoom(roomVO);
        userMapper.upsertRoomMember(roomVO.getMsgrId(), currentUserId, "owner");
        userMapper.upsertRoomMember(roomVO.getMsgrId(), targetUserId, "member");
        saveSystemMessageInternal(roomVO.getMsgrId(), currentUserId, "1:1 대화가 시작되었습니다.");
        return roomMapper.selectMyRoom(roomVO.getMsgrId(), currentUserId);
    }

    @Override
    @Transactional
    public MessengerRoomVO findOrCreateSelfRoom(String userId) {
        MessengerRoomVO existingRoom = roomMapper.findSelfRoom(userId);
        if (existingRoom != null) {
            return roomMapper.selectMyRoom(existingRoom.getMsgrId(), userId);
        }

        MessengerRoomVO roomVO = new MessengerRoomVO();
        roomVO.setRoomTypeCd("self");
        roomVO.setMsgrNm("나와의 채팅");
        roomMapper.insertMessengerRoom(roomVO);
        userMapper.upsertRoomMember(roomVO.getMsgrId(), userId, "owner");
        saveSystemMessageInternal(roomVO.getMsgrId(), userId, "나만 보는 메모 채팅방이 생성되었습니다.");
        return roomMapper.selectMyRoom(roomVO.getMsgrId(), userId);
    }

    @Override
    @Transactional
    public MessengerContentVO saveMessage(MessengerContentVO message) {
        requireRoomMembership(message.getMsgrId(), message.getUserId());
        message.setMsgContId(newMessageId());
        message.setSendDt(new Date());
        if (message.getReadYn() == null) {
            message.setReadYn("N");
        }
        if (message.getDelYn() == null) {
            message.setDelYn("N");
        }
        if (StringUtils.isBlank(message.getMsgTypeCd())) {
            message.setMsgTypeCd(StringUtils.isNotBlank(message.getMsgFileId()) ? "file" : "text");
        }
        contentMapper.insertMessengerContent(message);
        return enrichMessage(contentMapper.selectMessengerContent(message.getMsgContId(), message.getUserId()));
    }

    @Override
    @Transactional
    public MessengerContentVO saveFileMessage(String msgrId, String userId, String contents, List<MultipartFile> files) {
        requireRoomMembership(msgrId, userId);
        if (files == null || files.stream().noneMatch(file -> file != null && !file.isEmpty())) {
            throw new IllegalArgumentException("첨부할 파일을 선택하세요.");
        }

        MessengerContentVO message = new MessengerContentVO();
        message.setMsgrId(msgrId);
        message.setUserId(userId);
        message.setContents(StringUtils.defaultIfBlank(contents, buildFileSummary(files)));
        message.setFileList(files.stream().filter(file -> file != null && !file.isEmpty()).collect(Collectors.toList()));
        fileUploadService.saveFileS3(message, FileFolderType.MESSAGE.toString());
        message.setMsgTypeCd("file");
        return saveMessage(message);
    }

    @Override
    @Transactional
    public void inviteUsers(String msgrId, List<String> userIds, String requesterUserId) {
        requireRoomMembership(msgrId, requesterUserId);
        for (String userId : normalizeUserIds(userIds, null)) {
            userMapper.upsertRoomMember(msgrId, userId, "member");
        }
        saveSystemMessageInternal(msgrId, requesterUserId, "참여자가 초대되었습니다.");
    }

    @Override
    @Transactional
    public void kickUser(String msgrId, String targetUserId, String requesterUserId) {
        MessengerRoomVO room = requireRoom(msgrId, requesterUserId);
        if (!"owner".equalsIgnoreCase(room.getCurrentUserRole())) {
            throw new IllegalArgumentException("강퇴 권한이 없습니다.");
        }
        if (Objects.equals(targetUserId, requesterUserId)) {
            throw new IllegalArgumentException("자기 자신은 강퇴할 수 없습니다.");
        }
        userMapper.updateLeftTime(targetUserId, msgrId);
        saveSystemMessageInternal(msgrId, requesterUserId, findUserNmByUserId(targetUserId) + " 님이 대화방에서 제거되었습니다.");
    }

    @Override
    public void markAllAsRead(String msgrId, String userId) {
        requireRoomMembership(msgrId, userId);
        readMapper.insertReadRecords(msgrId, userId);
    }

    @Override
    public void updateRoomName(String msgrId, String msgrNm, String requesterUserId) {
        requireRoomMembership(msgrId, requesterUserId);
        if (StringUtils.isBlank(msgrNm)) {
            throw new IllegalArgumentException("채팅방 이름을 입력하세요.");
        }
        roomMapper.updateMessengerRoomName(msgrId, msgrNm.trim());
    }

    @Override
    public int getRoomParticipantCount(String msgrId) {
        return userMapper.countRoomUsers(msgrId);
    }

    @Override
    public List<MessengerParticipantVO> getRoomParticipants(String msgrId, String userId) {
        requireRoomMembership(msgrId, userId);
        return userMapper.selectRoomParticipants(msgrId, userId);
    }

    @Override
    @Transactional
    public void updateLeftTime(String msgrId, String userId) {
        MessengerRoomVO room = requireRoom(msgrId, userId);
        userMapper.updateLeftTime(userId, msgrId);
        promoteNextOwnerIfNeeded(msgrId, room.getOwnerUserId(), userId);
        saveSystemMessageInternal(msgrId, userId, "대화방을 나갔습니다.");
    }

    @Override
    @Transactional
    public void markAsRead(String msgContId) {
        contentMapper.updateReadStatus(msgContId);
    }

    @Override
    @Transactional
    public MessengerContentVO deleteMessage(String msgrId, String msgContId, String userId) {
        requireRoomMembership(msgrId, userId);
        MessengerContentVO message = contentMapper.selectMessengerContent(msgContId, userId);
        if (message == null || !Objects.equals(message.getMsgrId(), msgrId)) {
            throw new IllegalArgumentException("메시지를 찾을 수 없습니다.");
        }
        int updated = contentMapper.softDeleteMessengerContent(msgContId, userId);
        if (updated == 0) {
            throw new IllegalArgumentException("메시지 삭제 권한이 없습니다.");
        }
        if (Objects.equals(requireRoom(msgrId, userId).getPinnedMsgContId(), msgContId)) {
            roomMapper.clearPinnedMessage(msgrId);
        }
        return message;
    }

    @Override
    @Transactional
    public MessengerContentVO forwardMessage(String msgContId, String targetRoomId, String userId) {
        requireRoomMembership(targetRoomId, userId);
        MessengerContentVO original = findAccessibleMessage(msgContId, userId);
        if (original == null) {
            throw new IllegalArgumentException("전달할 메시지를 찾을 수 없습니다.");
        }

        MessengerContentVO forwarded = new MessengerContentVO();
        forwarded.setMsgrId(targetRoomId);
        forwarded.setUserId(userId);
        forwarded.setContents(StringUtils.defaultIfBlank(original.getContents(), original.getMsgTypeCd().equalsIgnoreCase("file") ? buildAttachmentLabel(original) : "전달된 메시지"));
        forwarded.setMsgTypeCd(StringUtils.defaultIfBlank(original.getMsgTypeCd(), "text"));
        forwarded.setForwardFromMsgContId(original.getMsgContId());
        if (StringUtils.isNotBlank(original.getMsgFileId())) {
            fileUploadService.copyFiles(original.getMsgFileId(), forwarded, FileFolderType.MESSAGE.toString());
        }
        return saveMessage(forwarded);
    }

    @Override
    @Transactional
    public MessengerContentVO pinMessage(String msgrId, String msgContId, String userId) {
        requireRoomMembership(msgrId, userId);
        MessengerContentVO message = contentMapper.selectMessengerContent(msgContId, userId);
        if (message == null || !Objects.equals(message.getMsgrId(), msgrId)) {
            throw new IllegalArgumentException("공지할 메시지를 찾을 수 없습니다.");
        }
        roomMapper.updatePinnedMessage(msgrId, msgContId, userId);
        return enrichMessage(message);
    }

    @Override
    @Transactional
    public void clearPinnedMessage(String msgrId, String userId) {
        requireRoomMembership(msgrId, userId);
        roomMapper.clearPinnedMessage(msgrId);
    }

    @Override
    @Transactional
    public void updateNotify(String msgrId, String userId, boolean notifyEnabled) {
        requireRoomMembership(msgrId, userId);
        userMapper.updateNotify(userId, msgrId, notifyEnabled ? "Y" : "N");
    }

    private MessengerRoomVO requireRoom(String msgrId, String userId) {
        MessengerRoomVO room = roomMapper.selectMyRoom(msgrId, userId);
        if (room == null) {
            throw new IllegalArgumentException("대화방을 찾을 수 없습니다.");
        }
        return room;
    }

    private MessengerUserVO requireRoomMembership(String msgrId, String userId) {
        MessengerUserVO membership = userMapper.selectRoomMembership(msgrId, userId);
        if (membership == null || membership.getLeftDt() != null) {
            throw new IllegalArgumentException("대화방 접근 권한이 없습니다.");
        }
        return membership;
    }

    private List<MessengerContentVO> enrichMessages(List<MessengerContentVO> messages) {
        if (messages == null) {
            return List.of();
        }
        return messages.stream().map(this::enrichMessage).collect(Collectors.toList());
    }

    private MessengerContentVO enrichMessage(MessengerContentVO message) {
        if (message == null) {
            return null;
        }
        if (StringUtils.isNotBlank(message.getMsgFileId())) {
            message.setAttachments(fileDetailService.readFileDetailList(message.getMsgFileId()));
        } else {
            message.setAttachments(List.of());
        }
        return message;
    }

    private MessengerContentVO findAccessibleMessage(String msgContId, String userId) {
        MessengerContentVO directMessage = enrichMessage(contentMapper.selectMessengerContent(msgContId, userId));
        if (directMessage != null) {
            requireRoomMembership(directMessage.getMsgrId(), userId);
            return directMessage;
        }

        MessengerContentVO fallbackMessage = contentMapper.selectMessengerContentList().stream()
            .filter(message -> Objects.equals(message.getMsgContId(), msgContId))
            .findFirst()
            .orElse(null);

        if (fallbackMessage == null) {
            return null;
        }

        requireRoomMembership(fallbackMessage.getMsgrId(), userId);
        return enrichMessage(fallbackMessage);
    }

    private void saveSystemMessageInternal(String msgrId, String userId, String contents) {
        MessengerContentVO systemMessage = new MessengerContentVO();
        systemMessage.setMsgrId(msgrId);
        systemMessage.setUserId(userId);
        systemMessage.setContents(contents);
        systemMessage.setMsgTypeCd("system");
        saveMessage(systemMessage);
    }

    private void promoteNextOwnerIfNeeded(String msgrId, String currentOwnerUserId, String leavingUserId) {
        if (!Objects.equals(currentOwnerUserId, leavingUserId)) {
            return;
        }
        List<MessengerParticipantVO> remaining = userMapper.selectRoomParticipants(msgrId, leavingUserId);
        remaining.stream()
            .filter(participant -> !Objects.equals(participant.getUserId(), leavingUserId))
            .findFirst()
            .ifPresent(nextOwner -> userMapper.upsertRoomMember(msgrId, nextOwner.getUserId(), "owner"));
    }

    private List<String> normalizeUserIds(List<String> userIds, String requiredUserId) {
        Set<String> normalized = new LinkedHashSet<>();
        if (StringUtils.isNotBlank(requiredUserId)) {
            normalized.add(requiredUserId);
        }
        if (userIds != null) {
            for (String userId : userIds) {
                if (StringUtils.isNotBlank(userId)) {
                    normalized.add(userId.trim());
                }
            }
        }
        return new ArrayList<>(normalized);
    }

    private boolean filterByScope(MessengerRoomVO room, String scope) {
        String normalizedScope = StringUtils.defaultIfBlank(scope, "all");
        return switch (normalizedScope) {
            case "unread" -> room.getUnreadCount() > 0;
            default -> true;
        };
    }

    private boolean filterByType(MessengerRoomVO room, String type) {
        String normalizedType = StringUtils.defaultIfBlank(type, "all");
        if ("all".equalsIgnoreCase(normalizedType)) {
            return true;
        }
        return normalizedType.equalsIgnoreCase(StringUtils.defaultIfBlank(room.getRoomTypeCd(), "group"));
    }

    private boolean filterByKeyword(MessengerRoomVO room, String keyword) {
        if (StringUtils.isBlank(keyword)) {
            return true;
        }
        String lowered = keyword.toLowerCase();
        return StringUtils.defaultString(room.getMsgrNm()).toLowerCase().contains(lowered)
            || StringUtils.defaultString(room.getLastMsgCont()).toLowerCase().contains(lowered);
    }

    private String defaultRoomName(String roomTypeCd) {
        if ("community".equalsIgnoreCase(roomTypeCd)) {
            return "커뮤니티 채팅";
        }
        return "새 그룹 채팅";
    }

    private String buildFileSummary(List<MultipartFile> files) {
        return files.stream()
            .filter(file -> file != null && !file.isEmpty())
            .map(MultipartFile::getOriginalFilename)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.joining(", ", "파일 공유: ", ""));
    }

    private String buildAttachmentLabel(MessengerContentVO message) {
        if (message.getAttachments() == null || message.getAttachments().isEmpty()) {
            return "첨부 파일";
        }
        return message.getAttachments().stream()
            .map(item -> StringUtils.defaultIfBlank(item.getOrgnFileNm(), item.getSaveFileNm()))
            .collect(Collectors.joining(", ", "파일 공유: ", ""));
    }

    private String newMessageId() {
        return "M" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private String findUserNmByUserId(String userId) {
        try {
            UsersVO userVO = usersMapper.selectUser(userId);
            if (userVO != null && userVO.getUserNm() != null) {
                return userVO.getUserNm();
            }
            return userId;
        } catch (Exception e) {
            log.error("사용자 조회 실패: {}", userId, e);
            return userId;
        }
    }
}
