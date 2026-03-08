package kr.or.ddit.email.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.comm.file.FileFolderType;
import kr.or.ddit.comm.file.service.impl.FileUploadServiceImpl;
import kr.or.ddit.email.content.service.EmailContentService;
import kr.or.ddit.mybatis.mapper.EmailBoxMapper;
import kr.or.ddit.mybatis.mapper.EmailContentMapper;
import kr.or.ddit.mybatis.mapper.EmailMappingMapper;
import kr.or.ddit.mybatis.mapper.EmailReceiverMapper;
import kr.or.ddit.mybatis.mapper.EmailSenderPartyMapper;
import kr.or.ddit.mybatis.mapper.FileMasterMapper; // FileMasterMapper 주입
import kr.or.ddit.vo.EmailContentVO;
import kr.or.ddit.vo.EmailMappingVO;
import kr.or.ddit.vo.EmailReceiverVO;
import kr.or.ddit.vo.EmailSenderPartyVO;
import kr.or.ddit.vo.FileMasterVO;
import kr.or.ddit.websocket.service.impl.NotificationServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

/**
 *
 * @author 홍현택
 * @since 2025. 10. 13.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------	   	-------------	   ---------------------------
 *  2025. 10. 13.     	홍현택	          최초 생성
 *  2025. 10. 13.		홍현택			이메일 발송, 임시저장 구현체 추가
 *  2025. 10. 13. 		홍현택			첨부파일 전송 추가
 *  2025. 10. 14.		홍현택			답장, 전달 메서드 구현체 추가
 *  2025. 10. 16.		홍현택			메일 전송시 알림 추가
 *  2025. 10. 16.		홍현택			휴지통에서 이메일 복원 구현체 추가
 * </pre>
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class EmailSendServiceImpl implements EmailSendService {

    private final EmailContentMapper emailContentMapper;
    private final EmailReceiverMapper emailReceiverMapper;
    private final EmailSenderPartyMapper emailSenderPartyMapper;
    private final EmailMappingMapper emailMappingMapper;
    private final EmailBoxMapper emailBoxMapper; // EmailBoxMapper 주입
    private final FileMasterMapper fileMasterMapper; // FileMasterMapper 주입
    private final FileUploadServiceImpl fileUploadService; // FileUploadServiceImpl 주입
    private final EmailContentService emailContentService; // EmailContentService 주입
    private final NotificationServiceImpl notificationService; // NotificationServiceImpl 주입

    @Transactional
    @Override
    public int sendEmail(EmailContentVO emailContentVO) {
        if (emailContentVO == null) {
            throw new IllegalArgumentException("부적절한 접근입니다.");
        }
        emailContentVO.setSendDate(LocalDateTime.now()); // 발송일자 설정

        String emailContId = emailContentVO.getEmailContId();
        String senderId = emailContentVO.getUserId();

        // 파일 업로드 처리
        if (emailContentVO.getFileList() != null && !emailContentVO.getFileList().isEmpty()) {
            // 기존 파일이 있다면 삭제 처리 (DEL_YN = 'Y')
            if (emailContentVO.getMailFileId() != null && !emailContentVO.getMailFileId().isEmpty()) {
                FileMasterVO fileMasterToDelete = new FileMasterVO();
                fileMasterToDelete.setFileId(emailContentVO.getMailFileId());
                fileMasterMapper.updateFileMasterDelyn(fileMasterToDelete);
            }
            // 새 파일 업로드 및 mailFileId 설정
            fileUploadService.saveFileS3(emailContentVO, FileFolderType.MAIL.toString());
        }

        int cnt = 0;

        if (emailContId != null && !emailContId.isEmpty()) {
            // 기존 메일 업데이트
            cnt = emailContentMapper.updateEmailContent(emailContentVO);
            // 기존 발신자, 수신자, 매핑 정보 삭제
            emailSenderPartyMapper.deleteEmailSenderPartyByEmailId(emailContId);
            emailReceiverMapper.deleteEmailReceiversByEmailId(emailContId);
            emailMappingMapper.deleteEmailMappingsByEmailId(emailContId);
        } else {
            // 새 메일 삽입
            cnt = emailContentMapper.insertEmailContent(emailContentVO);
            emailContId = emailContentVO.getEmailContId(); // 새로 생성된 ID 가져오기
        }

        if (cnt == 0) return 0; // 업데이트 또는 삽입 실패 시

        final String finalEmailContId = emailContId; // 람다에서 사용하기 위한 final 변수

        // 2. EMAIL_SENDER_PARTY 테이블에 발신자 정보 삽입
        EmailSenderPartyVO senderParty = new EmailSenderPartyVO();
        senderParty.setEmailContId(finalEmailContId);
        senderParty.setUserId(senderId);
        emailSenderPartyMapper.insertEmailSenderParty(senderParty);

        // 3. 받는 사람 처리
        String[] recipients = emailContentVO.getRecipients();
        if (recipients != null && recipients.length > 0) {
            List<EmailReceiverVO> receiverList = Arrays.stream(recipients)
                .map(recipientId -> {
                    EmailReceiverVO receiver = new EmailReceiverVO();
                    receiver.setEmailContId(finalEmailContId);
                    receiver.setUserId(recipientId.trim()); // 공백 제거
                    return receiver;
                })
                .collect(Collectors.toList());
            emailReceiverMapper.insertEmailReceivers(receiverList);

            // 4. 받는 사람들의 받은편지함에 메일 매핑
            for (String recipientId : recipients) {
                Map<String, Object> params = new HashMap<>();
                params.put("userId", recipientId.trim());
                params.put("mailboxTypeCd", "G101"); // G101: 받은편지함
                String mailboxId = emailBoxMapper.findMailboxIdByUserIdAndType(params);

                if (mailboxId != null) {
                    EmailMappingVO mapping = new EmailMappingVO();
                    mapping.setEmailContId(finalEmailContId);
                    mapping.setMailboxId(mailboxId);
                    emailMappingMapper.insertEmailMapping(mapping);
                }

                // 중요 메일인 경우, 받는 사람의 중요 편지함(G104)에도 매핑 추가
                if ("Y".equals(emailContentVO.getImptMailYn())) {
                    Map<String, Object> importantParams = new HashMap<>();
                    importantParams.put("userId", recipientId.trim());
                    importantParams.put("mailboxTypeCd", "G104"); // G104: 중요 편지함
                    String importantMailboxId = emailBoxMapper.findMailboxIdByUserIdAndType(importantParams);

                    if (importantMailboxId != null) {
                        EmailMappingVO importantMapping = new EmailMappingVO();
                        importantMapping.setEmailContId(finalEmailContId);
                        importantMapping.setMailboxId(importantMailboxId);
                        emailMappingMapper.insertEmailMapping(importantMapping);
                    } else {
                        log.warn("사용자 {}의 중요 편지함(G104)을 찾을 수 없습니다. 중요 메일 매핑 실패.", recipientId.trim());
                    }
                }

                // ================ '새 메일 도착' 알림 발송 =================
                Map<String, Object> payload = new HashMap<>();
                payload.put("receiverId", recipientId.trim());
                payload.put("senderId", senderId);
                payload.put("alarmCode", "MAIL_01"); // 새 메일 도착 알림 코드
                payload.put("pk", finalEmailContId);
                notificationService.sendNotification(payload);
                // =======================================================
            }
        }

        // 5. 보낸 사람의 보낸편지함에 메일 매핑
        Map<String, Object> params = new HashMap<>();
        params.put("userId", senderId);
        params.put("mailboxTypeCd", "G102"); // G102: 보낸편지함
        String sentMailboxId = emailBoxMapper.findMailboxIdByUserIdAndType(params);
        if (sentMailboxId != null) {
            EmailMappingVO senderMapping = new EmailMappingVO();
            senderMapping.setEmailContId(finalEmailContId);
            senderMapping.setMailboxId(sentMailboxId);
            emailMappingMapper.insertEmailMapping(senderMapping);
        }

        return cnt;
    }

    @Transactional
    @Override
    public int replyEmail(String originalEmailContId, EmailContentVO emailContentVO, String[] recipients) {
        // 1. 원본 메일 정보 조회 (필요시)
        // 현재는 emailContentVO에 답장 내용이 모두 담겨오므로, 원본 메일 정보는 UI에서만 활용하고
        // 여기서는 emailContentVO를 바로 sendEmail에 전달합니다.
        // 만약 원본 메일의 특정 정보(예: 첨부파일)를 서버에서 다시 가져와야 한다면
        // emailContentService.readEmailDetail(originalEmailContId, emailContentVO.getUserId()); 를 사용합니다.

        emailContentVO.setRecipients(recipients);

        // 2. sendEmail 메서드를 사용하여 발송 처리
        return sendEmail(emailContentVO);
    }

    @Transactional
    @Override
    public int forwardEmail(String originalEmailContId, EmailContentVO emailContentVO, String[] recipients) {
        // 1. 원본 메일 정보 조회 (첨부파일 복사를 위해 필요)
        EmailContentVO originalEmail = emailContentService.readEmailDetail(originalEmailContId, emailContentVO.getUserId());
        if (originalEmail == null) {
            throw new IllegalArgumentException("원본 메일을 찾을 수 없습니다.");
        }

        emailContentVO.setRecipients(recipients);

        // 2. 첨부 파일 복사 로직
        if (originalEmail.getMailFileId() != null && !originalEmail.getMailFileId().isEmpty()) {
            // emailContentVO에 원본 메일의 mailFileId를 설정하여 sendEmail에서 처리하도록 함
            emailContentVO.setMailFileId(originalEmail.getMailFileId());
            // fileUploadService.copyFiles(originalEmail.getMailFileId(), emailContentVO, FileFolderType.MAIL.toString());
        }

        // 3. sendEmail 메서드를 사용하여 발송 처리
        return sendEmail(emailContentVO);
    }

    @Override
    public boolean saveDraft(EmailContentVO emailContentVO, String[] recipients) {
        // 임시저장은 발송일자가 아닌 저장일자를 기록
        emailContentVO.setSendDate(LocalDateTime.now());

        String emailContId = emailContentVO.getEmailContId();
        String senderId = emailContentVO.getUserId();

        // 파일 업로드 처리 (임시저장 시 첨부파일 제외 정책에 따라 처리하지 않음)
        // if (emailContentVO.getFileList() != null && !emailContentVO.getFileList().isEmpty()) {
        //     // 기존 파일이 있다면 삭제 처리 (DEL_YN = 'Y')
        //     if (emailContentVO.getMailFileId() != null && !emailContentVO.getMailFileId().isEmpty()) {
        //         FileMasterVO fileMasterToDelete = new FileMasterVO();
        //         fileMasterToDelete.setFileId(emailContentVO.getMailFileId());
        //         fileMasterMapper.updateFileMasterDelyn(fileMasterToDelete);
        //     }
        //     // 새 파일 업로드 및 mailFileId 설정
        //     fileUploadService.saveFileS3(emailContentVO, FileFolderType.MAIL.toString());
        // }

        int cnt = 0;

        if (emailContId != null && !emailContId.isEmpty()) {
            // 기존 임시 메일 업데이트
            cnt = emailContentMapper.updateEmailContent(emailContentVO);
            // 기존 발신자, 수신자, 매핑 정보 삭제
            emailSenderPartyMapper.deleteEmailSenderPartyByEmailId(emailContId);
            emailReceiverMapper.deleteEmailReceiversByEmailId(emailContId);
            emailMappingMapper.deleteEmailMappingsByEmailId(emailContId);
        } else {
            // 새 임시 메일 삽입
            cnt = emailContentMapper.insertEmailContent(emailContentVO);
            emailContId = emailContentVO.getEmailContId(); // 새로 생성된 ID 가져오기
        }

        if (cnt == 0) return false; // 업데이트 또는 삽입 실패 시

        final String finalEmailContId = emailContId;

        // 발신자 정보 저장
        EmailSenderPartyVO senderParty = new EmailSenderPartyVO();
        senderParty.setEmailContId(finalEmailContId);
        senderParty.setUserId(senderId);
        emailSenderPartyMapper.insertEmailSenderParty(senderParty);

        // 수신자 정보 저장
        if (recipients != null && recipients.length > 0) {
            List<EmailReceiverVO> receiverList = Arrays.stream(recipients)
                .map(recipientId -> {
                    EmailReceiverVO receiver = new EmailReceiverVO();
                    receiver.setEmailContId(finalEmailContId);
                    receiver.setUserId(recipientId.trim()); // 공백 제거
                    return receiver;
                })
                .collect(Collectors.toList());
            emailReceiverMapper.insertEmailReceivers(receiverList);
        }

        // 발신자의 임시 보관함(G103)에 메일 매핑
        Map<String, Object> params = new HashMap<>();
        params.put("userId", senderId);
        params.put("mailboxTypeCd", "G103"); // G103: 임시 보관함
        String draftMailboxId = emailBoxMapper.findMailboxIdByUserIdAndType(params);

        if (draftMailboxId != null) {
            EmailMappingVO senderMapping = new EmailMappingVO();
            senderMapping.setEmailContId(emailContId);
            senderMapping.setMailboxId(draftMailboxId);
            emailMappingMapper.insertEmailMapping(senderMapping);
            return true;
        } else {
            // 임시 보관함을 찾을 수 없는 경우
            log.warn("사용자 {}의 임시 보관함(G103)을 찾을 수 없습니다.", senderId);
            return false;
        }
    }

    @Transactional
    @Override
    public int deleteEmails(String[] emailContIds, String mailboxTypeCd, String userId) {
        int deletedCount = 0;
        String trashMailboxId = emailBoxMapper.findMailboxIdByUserIdAndType(
            Map.of("userId", userId, "mailboxTypeCd", "G105")
        );

        if (trashMailboxId == null) {
            log.error("사용자 {}의 휴지통(G105)을 찾을 수 없습니다.", userId);
            return 0;
        }

        for (String emailContId : emailContIds) {
            if ("G105".equals(mailboxTypeCd)) { // 휴지통에서 삭제 -> 영구 삭제
                // 1. 해당 사용자의 매핑 정보의 DEL_YN을 'Y'로 업데이트
                Map<String, Object> updateDelYnParams = new HashMap<>();
                updateDelYnParams.put("emailContId", emailContId);
                updateDelYnParams.put("userId", userId);
                deletedCount += emailMappingMapper.updateEmailMappingDelYn(updateDelYnParams);

                // 2. (선택 사항) 해당 이메일 콘텐츠에 대한 모든 매핑이 DEL_YN='Y'인 경우 EMAIL_CONTENT를 물리적으로 삭제하는 로직은
                // 별도의 배치 작업으로 처리하는 것이 더 안전합니다. 여기서는 매핑의 DEL_YN만 업데이트합니다.
            } else { // 다른 메일함에서 삭제 -> 휴지통으로 이동 (소프트 삭제)
                Map<String, Object> currentMappingParams = new HashMap<>();
                currentMappingParams.put("emailContId", emailContId);
                currentMappingParams.put("userId", userId);
                currentMappingParams.put("mailboxTypeCd", mailboxTypeCd); // 현재 메일함 유형도 전달
                EmailMappingVO currentMapping =
                    emailMappingMapper.selectEmailMappingByEmailContIdAndUserIdAndMailboxTypeCd(currentMappingParams);

                if (currentMapping != null) {
                    // 이미 휴지통에 있는지 확인
                    if (currentMapping.getMailboxId().equals(trashMailboxId)) {
                        // 이미 휴지통에 있으므로, 이 이메일은 건너뜁니다.
                        continue;
                    }

                    // 기존 매핑 삭제
                    emailMappingMapper.deleteEmailMapping(currentMapping);

                    // 해당 emailContId가 이미 휴지통에 매핑되어 있는지 확인
                    Map<String, Object> existingTrashMappingParams = new HashMap<>();
                    existingTrashMappingParams.put("emailContId", emailContId);
                    existingTrashMappingParams.put("userId", userId);
                    existingTrashMappingParams.put("mailboxTypeCd", "G105"); // 휴지통
                    EmailMappingVO existingTrashMapping =
                        emailMappingMapper.selectEmailMappingByEmailContIdAndUserIdAndMailboxTypeCd(existingTrashMappingParams);

                    if (existingTrashMapping == null) {
                        // 휴지통에 새 매핑 삽입
                        EmailMappingVO newTrashMapping = new EmailMappingVO();
                        newTrashMapping.setEmailContId(emailContId);
                        newTrashMapping.setMailboxId(trashMailboxId);
                        newTrashMapping.setReadYn(currentMapping.getReadYn()); // 기존 읽음 상태 유지
                        newTrashMapping.setDelYn("N"); // 휴지통에 있으므로 DEL_YN은 'N'
                        emailMappingMapper.insertEmailMapping(newTrashMapping);
                    }
                    deletedCount++;
                }
            }
        }
        return deletedCount;
    }

    @Transactional
    @Override
    public int deleteAllEmails(String mailboxTypeCd, String userId) {
        int totalDeletedCount = 0;
        // 특정 메일함의 모든 이메일 매핑 정보를 가져옴
        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);
        params.put("mailboxTypeCd", mailboxTypeCd);
        List<EmailMappingVO> emailMappings =
            emailMappingMapper.selectEmailMappingsByMailboxTypeCdAndUserId(params);

        if (emailMappings != null && !emailMappings.isEmpty()) {
            String[] emailContIds = emailMappings.stream()
                .map(EmailMappingVO::getEmailContId)
                .toArray(String[]::new);
            totalDeletedCount = deleteEmails(emailContIds, mailboxTypeCd, userId);
        }
        return totalDeletedCount;
    }

    @Transactional
    @Override
    public int restoreEmails(String[] emailContIds, String userId) {
        int restoredCount = 0;
        // 사용자의 받은 편지함 ID 조회
        String inboxMailboxId = emailBoxMapper.findMailboxIdByUserIdAndType(
            Map.of("userId", userId, "mailboxTypeCd", "G101")
        );

        if (inboxMailboxId == null) {
            log.error("사용자 {}의 받은 편지함(G101)을 찾을 수 없습니다.", userId);
            return 0;
        }

        for (String emailContId : emailContIds) {
            // 휴지통에 있는 해당 이메일의 매핑 정보 조회
            Map<String, Object> params = new HashMap<>();
            params.put("emailContId", emailContId);
            params.put("userId", userId);
            params.put("mailboxTypeCd", "G105"); // 휴지통
            EmailMappingVO trashMapping =
                emailMappingMapper.selectEmailMappingByEmailContIdAndUserIdAndMailboxTypeCd(params);

            if (trashMapping != null) {
                // EmailContentVO를 조회하여 보낸 메일인지 받은 메일인지 판단
                EmailContentVO emailContent = emailContentService.readEmailDetail(emailContId, userId); // userId는 읽음 상태 변경에 사용되지만, 여기서는 내용 조회를 위해 사용
                String targetMailboxTypeCd = null; // 복원할 메일함 유형 코드
                boolean isSender = emailContent.getUserId() != null && emailContent.getUserId().equals(userId);
                boolean isReceiver = emailContent.getReceiverList() != null &&
                                     emailContent.getReceiverList().stream().anyMatch(r -> r.getUserId().equals(userId));

                List<String> mailboxesToRestore = new java.util.ArrayList<>();

                if (isSender) {
                    mailboxesToRestore.add("G102"); // G102: 보낸 편지함
                }
                if (isReceiver) {
                    mailboxesToRestore.add("G101"); // G101: 받은 편지함
                }

                // 만약 발신자도 수신자도 아닌 경우 (예: 중요 메일함에서 바로 휴지통으로 간 경우 등) 기본적으로 받은 편지함으로 복원
                if (mailboxesToRestore.isEmpty()) {
                    mailboxesToRestore.add("G101");
                }

                // 기존 휴지통 매핑 삭제
                emailMappingMapper.deleteEmailMapping(trashMapping);

                for (String mailboxType : mailboxesToRestore) {
                    String targetMailboxId = emailBoxMapper.findMailboxIdByUserIdAndType(
                        Map.of("userId", userId, "mailboxTypeCd", mailboxType)
                    );

                    if (targetMailboxId == null) {
                        log.error("사용자 {}의 {} 메일함({})을 찾을 수 없습니다.", userId, mailboxType, mailboxType);
                        continue; // 다음 메일함으로 넘어감
                    }

                    // 대상 메일함에 새 매핑 삽입
                    EmailMappingVO newMapping = new EmailMappingVO();
                    newMapping.setEmailContId(emailContId);
                    newMapping.setMailboxId(targetMailboxId);
                    newMapping.setReadYn(trashMapping.getReadYn()); // 기존 읽음 상태 유지
                    newMapping.setDelYn("N"); // 복원 시 DEL_YN 초기화

                    // 이미 해당 메일함에 매핑이 존재하는지 확인 (중복 삽입 방지)
                    Map<String, Object> existingMappingParams = new HashMap<>();
                    existingMappingParams.put("emailContId", emailContId);
                    existingMappingParams.put("userId", userId);
                    existingMappingParams.put("mailboxTypeCd", mailboxType);
                    EmailMappingVO existingMapping = emailMappingMapper.selectEmailMappingByEmailContIdAndUserIdAndMailboxTypeCd(existingMappingParams);

                    if (existingMapping == null) {
                        int insertResult = emailMappingMapper.insertEmailMapping(newMapping);
                        if (insertResult > 0) {
                            restoredCount++;
                        }
                    } else {
                        // 이미 매핑이 존재하면 업데이트 (예: DEL_YN이 'Y'인 경우 'N'으로 변경)
                        existingMapping.setReadYn(newMapping.getReadYn());
                        existingMapping.setDelYn("N");
                        emailMappingMapper.updateEmailMapping(existingMapping);
                        restoredCount++;
                    }
                }
            }
        }
        return restoredCount;
    }
}
