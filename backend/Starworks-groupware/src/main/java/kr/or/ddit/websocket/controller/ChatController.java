package kr.or.ddit.websocket.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import kr.or.ddit.security.CustomUserDetails;
import kr.or.ddit.vo.ChatMessageDTO;
import kr.or.ddit.vo.MessengerContentVO;
import kr.or.ddit.vo.MessengerPanelVO;
import kr.or.ddit.vo.MessengerParticipantVO;
import kr.or.ddit.vo.MessengerRoomDetailVO;
import kr.or.ddit.vo.MessengerRoomVO;
import kr.or.ddit.vo.UsersVO;
import kr.or.ddit.websocket.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;

    @GetMapping("/current-user")
    @ResponseBody
    public UsersVO getCurrentUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return new UsersVO();
        }
        return chatService.getCurrentUser(userDetails.getUsername());
    }

    @GetMapping("/users")
    @ResponseBody
    public List<UsersVO> getUserList(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return chatService.getAvailableUsers(userDetails.getUsername());
    }

    @GetMapping("/panel")
    @ResponseBody
    public MessengerPanelVO getPanel(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return chatService.getPanel(userDetails.getUsername());
    }

    @GetMapping("/rooms")
    @ResponseBody
    public List<MessengerRoomVO> getMyRooms(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestParam(required = false) String scope,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String type
    ) {
        return chatService.getMyRooms(userDetails.getUsername(), scope, keyword, type);
    }

    @GetMapping("/room/{msgrId}")
    @ResponseBody
    public MessengerRoomDetailVO getRoomDetail(
        @PathVariable String msgrId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return chatService.getRoomDetail(msgrId, userDetails.getUsername());
    }

    @GetMapping("/room/{msgrId}/messages")
    @ResponseBody
    public List<MessengerContentVO> getRoomMessages(
        @PathVariable String msgrId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return chatService.getRoomMessages(msgrId, userDetails.getUsername());
    }

    @GetMapping("/room/{msgrId}/search")
    @ResponseBody
    public List<MessengerContentVO> searchMessages(
        @PathVariable String msgrId,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestParam String q
    ) {
        return chatService.searchRoomMessages(msgrId, userDetails.getUsername(), q);
    }

    @GetMapping("/room/findOrCreate")
    @ResponseBody
    public MessengerRoomVO findOrCreateRoom(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestParam String userId
    ) {
        return chatService.findOrCreatePrivateRoom(userDetails.getUsername(), userId);
    }

    @PostMapping("/room/self")
    @ResponseBody
    public MessengerRoomVO openSelfRoom(@AuthenticationPrincipal CustomUserDetails userDetails) {
        MessengerRoomVO room = chatService.findOrCreateSelfRoom(userDetails.getUsername());
        broadcastRoomRefresh(room.getMsgrId(), userDetails.getUsername());
        return room;
    }

    @PostMapping("/room/create")
    @ResponseBody
    @SuppressWarnings("unchecked")
    public MessengerRoomVO createChatRoom(
        @RequestBody Map<String, Object> request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<String> userIds = (List<String>) request.get("userIds");
        String roomNm = (String) request.get("roomNm");
        String roomTypeCd = (String) request.getOrDefault("roomTypeCd", Boolean.TRUE.equals(request.get("isGroup")) ? "group" : "private");

        MessengerRoomVO room;
        if ("private".equalsIgnoreCase(roomTypeCd) && userIds != null && userIds.size() == 1) {
            room = chatService.findOrCreatePrivateRoom(userDetails.getUsername(), userIds.get(0));
        } else {
            room = chatService.createGroupRoom(userIds, roomNm, userDetails.getUsername(), roomTypeCd);
        }
        broadcastRoomRefresh(room.getMsgrId(), userDetails.getUsername());
        return room;
    }

    @PostMapping("/room/{msgrId}/invite")
    @ResponseBody
    @SuppressWarnings("unchecked")
    public Map<String, Object> inviteUsers(
        @PathVariable String msgrId,
        @RequestBody Map<String, Object> body,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<String> userIds = (List<String>) body.getOrDefault("userIds", List.of());
        chatService.inviteUsers(msgrId, userIds, userDetails.getUsername());
        broadcastRoomRefresh(msgrId, userDetails.getUsername());
        return Map.of("success", true);
    }

    @PostMapping("/room/{msgrId}/kick")
    @ResponseBody
    public Map<String, Object> kickUser(
        @PathVariable String msgrId,
        @RequestBody Map<String, String> body,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        chatService.kickUser(msgrId, body.get("userId"), userDetails.getUsername());
        broadcastRoomRefresh(msgrId, userDetails.getUsername());
        return Map.of("success", true);
    }

    @PostMapping("/room/markAsRead/{msgrId}")
    @ResponseBody
    public ResponseEntity<String> markRoomMessagesAsRead(
        @PathVariable String msgrId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        try {
            chatService.markAllAsRead(msgrId, userDetails.getUsername());
            broadcastRoomRefresh(msgrId, userDetails.getUsername());
            return ResponseEntity.ok("Read status updated successfully.");
        } catch (Exception e) {
            log.error("메시지 읽음 처리 실패 - msgrId={}", msgrId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update read status.");
        }
    }

    @PostMapping("/room/{msgrId}/name")
    @ResponseBody
    public Map<String, Object> updateRoomName(
        @PathVariable String msgrId,
        @RequestBody Map<String, String> requestBody,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        chatService.updateRoomName(msgrId, requestBody.get("msgrNm"), userDetails.getUsername());
        broadcastRoomRefresh(msgrId, userDetails.getUsername());
        return Map.of("success", true);
    }

    @GetMapping("/room/{msgrId}/participants")
    @ResponseBody
    public List<MessengerParticipantVO> getRoomParticipants(
        @PathVariable String msgrId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return chatService.getRoomParticipants(msgrId, userDetails.getUsername());
    }

    @PatchMapping("/room/{msgrId}/notify")
    @ResponseBody
    public Map<String, Object> updateNotify(
        @PathVariable String msgrId,
        @RequestBody Map<String, Object> body,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        boolean enabled = Boolean.parseBoolean(String.valueOf(body.getOrDefault("notifyEnabled", true)));
        chatService.updateNotify(msgrId, userDetails.getUsername(), enabled);
        broadcastRoomRefresh(msgrId, userDetails.getUsername());
        return Map.of("success", true, "notifyEnabled", enabled);
    }

    @PatchMapping("/room/{msgrId}/pin")
    @ResponseBody
    public Map<String, Object> pinMessage(
        @PathVariable String msgrId,
        @RequestBody(required = false) Map<String, String> body,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        String msgContId = body == null ? null : body.get("msgContId");
        if (msgContId == null || msgContId.isBlank()) {
            chatService.clearPinnedMessage(msgrId, userDetails.getUsername());
            messagingTemplate.convertAndSend("/topic/room/" + msgrId, Map.of("type", "PIN", "msgContId", ""));
        } else {
            MessengerContentVO pinned = chatService.pinMessage(msgrId, msgContId, userDetails.getUsername());
            messagingTemplate.convertAndSend("/topic/room/" + msgrId, Map.of(
                "type", "PIN",
                "msgContId", pinned.getMsgContId(),
                "contents", pinned.getContents(),
                "userNm", pinned.getUserNm(),
                "msgTypeCd", pinned.getMsgTypeCd()
            ));
        }
        broadcastRoomRefresh(msgrId, userDetails.getUsername());
        return Map.of("success", true);
    }

    @PostMapping("/room/{msgrId}/leave")
    @ResponseBody
    public Map<String, Object> leaveRoom(
        @PathVariable String msgrId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        chatService.updateLeftTime(msgrId, userDetails.getUsername());
        messagingTemplate.convertAndSend("/topic/room/" + msgrId, Map.of("type", "LEAVE", "userId", userDetails.getUsername()));
        messagingTemplate.convertAndSend("/topic/chat/update", Map.of("type", "ROOM_UPDATE", "msgrId", msgrId));
        messagingTemplate.convertAndSend("/topic/chat/update/" + userDetails.getUsername(), Map.of("type", "ROOM_UPDATE", "msgrId", msgrId));
        return Map.of("success", true);
    }

    @DeleteMapping("/message/{msgContId}")
    @ResponseBody
    public Map<String, Object> deleteMessage(
        @PathVariable String msgContId,
        @RequestParam String msgrId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        MessengerContentVO deleted = chatService.deleteMessage(msgrId, msgContId, userDetails.getUsername());
        messagingTemplate.convertAndSend("/topic/room/" + msgrId, Map.of("type", "DELETE", "msgContId", msgContId));
        broadcastRoomRefresh(msgrId, userDetails.getUsername());
        return Map.of("success", true, "msgContId", deleted.getMsgContId());
    }

    @PostMapping("/message/{msgContId}/forward")
    @ResponseBody
    public MessengerContentVO forwardMessage(
        @PathVariable String msgContId,
        @RequestBody Map<String, String> body,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        MessengerContentVO forwarded = chatService.forwardMessage(msgContId, body.get("targetRoomId"), userDetails.getUsername());
        messagingTemplate.convertAndSend("/topic/room/" + forwarded.getMsgrId(), toSocketPayload(forwarded));
        broadcastRoomRefresh(forwarded.getMsgrId(), userDetails.getUsername());
        return forwarded;
    }

    @PostMapping("/room/{msgrId}/files")
    @ResponseBody
    public MessengerContentVO uploadFiles(
        @PathVariable String msgrId,
        @RequestPart(value = "files") List<MultipartFile> files,
        @RequestParam(required = false) String contents,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        MessengerContentVO message = chatService.saveFileMessage(msgrId, userDetails.getUsername(), contents, files);
        messagingTemplate.convertAndSend("/topic/room/" + msgrId, toSocketPayload(message));
        broadcastRoomRefresh(msgrId, userDetails.getUsername());
        return message;
    }

    @GetMapping("/room/{msgrId}/export.xlsx")
    @ResponseBody
    public ResponseEntity<byte[]> exportMessages(
        @PathVariable String msgrId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) throws IOException {
        List<MessengerContentVO> messages = chatService.getRoomMessagesForExport(msgrId, userDetails.getUsername());
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("messages");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("시간");
            header.createCell(1).setCellValue("보낸 사람");
            header.createCell(2).setCellValue("유형");
            header.createCell(3).setCellValue("내용");
            header.createCell(4).setCellValue("첨부 파일");

            int rowIndex = 1;
            for (MessengerContentVO message : messages) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(message.getSendDt() == null ? "" : message.getSendDt().toString());
                row.createCell(1).setCellValue(message.getUserNm() == null ? message.getUserId() : message.getUserNm());
                row.createCell(2).setCellValue(message.getMsgTypeCd());
                row.createCell(3).setCellValue(message.getContents() == null ? "" : message.getContents());
                row.createCell(4).setCellValue(message.getAttachments() == null ? "" : message.getAttachments().stream()
                    .map(item -> item.getOrgnFileNm() == null ? item.getSaveFileNm() : item.getOrgnFileNm())
                    .reduce((left, right) -> left + ", " + right)
                    .orElse(""));
            }

            for (int i = 0; i < 5; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDisposition(ContentDisposition.attachment()
                .filename("chat-" + msgrId + ".xlsx", StandardCharsets.UTF_8)
                .build());
            return ResponseEntity.ok().headers(headers).body(out.toByteArray());
        }
    }

    @MessageMapping("/chat.sendMessage/{msgrId}")
    public void sendMessage(
        @DestinationVariable String msgrId,
        @Payload ChatMessageDTO message,
        Principal principal
    ) {
        if (principal == null || principal.getName() == null) {
            return;
        }

        MessengerContentVO contentVO = new MessengerContentVO();
        contentVO.setMsgrId(msgrId);
        contentVO.setUserId(principal.getName());
        contentVO.setContents(message.getContents());
        contentVO.setMsgTypeCd(message.getMsgTypeCd());
        contentVO.setForwardFromMsgContId(message.getForwardFromMsgContId());
        MessengerContentVO savedMessage = chatService.saveMessage(contentVO);
        messagingTemplate.convertAndSend("/topic/room/" + msgrId, toSocketPayload(savedMessage));
        broadcastRoomRefresh(msgrId, principal.getName());
    }

    @MessageMapping("/chat.readMessage/{msgrId}")
    public void readMessage(
        @DestinationVariable String msgrId,
        @Payload String ignored,
        Principal principal
    ) {
        if (principal == null || principal.getName() == null) {
            return;
        }
        chatService.markAllAsRead(msgrId, principal.getName());
        messagingTemplate.convertAndSend("/topic/room/" + msgrId + "/read", principal.getName());
        broadcastRoomRefresh(msgrId, principal.getName());
    }

    @MessageMapping("/chat.addUser/{msgrId}")
    public void addUser(
        @DestinationVariable String msgrId,
        @Payload ChatMessageDTO message,
        Principal principal
    ) {
        if (principal == null || principal.getName() == null) {
            return;
        }
        messagingTemplate.convertAndSend("/topic/room/" + msgrId, Map.of("type", "JOIN", "userId", principal.getName()));
        broadcastRoomRefresh(msgrId, principal.getName());
    }

    @MessageMapping("/chat.removeUser/{msgrId}")
    public void removeUser(
        @DestinationVariable String msgrId,
        @Payload ChatMessageDTO message,
        Principal principal
    ) {
        if (principal == null || principal.getName() == null) {
            return;
        }
        messagingTemplate.convertAndSend("/topic/room/" + msgrId, Map.of("type", "LEAVE", "userId", principal.getName()));
        broadcastRoomRefresh(msgrId, principal.getName());
    }

    private Map<String, Object> toSocketPayload(MessengerContentVO message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "file".equalsIgnoreCase(message.getMsgTypeCd()) ? "FILE" : "CHAT");
        payload.put("msgContId", message.getMsgContId());
        payload.put("msgrId", message.getMsgrId());
        payload.put("userId", message.getUserId());
        payload.put("userNm", message.getUserNm());
        payload.put("contents", message.getContents());
        payload.put("sendDt", message.getSendDt());
        payload.put("msgTypeCd", message.getMsgTypeCd());
        payload.put("unreadCount", message.getUnreadCount());
        payload.put("userFilePath", message.getUserFilePath());
        payload.put("jbgdNm", message.getJbgdNm());
        payload.put("deptNm", message.getDeptNm());
        payload.put("forwardFromMsgContId", message.getForwardFromMsgContId());
        payload.put("forwardPreview", message.getForwardPreview());
        payload.put("attachments", message.getAttachments());
        return payload;
    }

    private void broadcastRoomRefresh(String msgrId, String requesterUserId) {
        messagingTemplate.convertAndSend("/topic/chat/update", Map.of("type", "ROOM_UPDATE", "msgrId", msgrId));
        try {
            List<MessengerParticipantVO> participants = chatService.getRoomParticipants(msgrId, requesterUserId);
            for (MessengerParticipantVO participant : participants) {
                messagingTemplate.convertAndSend("/topic/chat/update/" + participant.getUserId(), Map.of("type", "ROOM_UPDATE", "msgrId", msgrId));
            }
        } catch (Exception e) {
            messagingTemplate.convertAndSend("/topic/chat/update/" + requesterUserId, Map.of("type", "ROOM_UPDATE", "msgrId", msgrId));
        }
    }
}
