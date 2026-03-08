package kr.or.ddit.websocket.service.impl;

import java.util.Map;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import kr.or.ddit.alarm.log.service.AlarmLogService;
import kr.or.ddit.alarm.template.service.AlarmTemplateService;
import kr.or.ddit.mybatis.mapper.AuthorizationDocumentMapper;
import kr.or.ddit.mybatis.mapper.BoardMapper;
import kr.or.ddit.mybatis.mapper.EmailContentMapper;
import kr.or.ddit.mybatis.mapper.MainTaskMapper;
import kr.or.ddit.mybatis.mapper.ProjectMapper;
import kr.or.ddit.vo.AlarmLogVO;
import kr.or.ddit.vo.AlarmTemplateVO;
import kr.or.ddit.vo.AuthorizationDocumentVO;
import kr.or.ddit.vo.BoardVO;
import kr.or.ddit.vo.EmailContentVO;
import kr.or.ddit.vo.MainTaskVO;
import kr.or.ddit.vo.ProjectVO;
import kr.or.ddit.websocket.dto.NotificationTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl {

	private static final String DEFAULT_MESSAGE = "새로운 알림이 도착했습니다.";
	private static final String DEFAULT_URL = "#";

	private final SimpMessagingTemplate messagingTemplate;
	private final AlarmLogService alarmLogService;
	private final AlarmTemplateService alarmTemplateService;
	private final AuthorizationDocumentMapper approvalMapper;
	private final ProjectMapper projMapper;
	private final MainTaskMapper taskMapper;
	private final BoardMapper boardMapper;
	private final EmailContentMapper emailmapper;

	public void sendNotification(Map<String, Object> payload) {
		if (payload == null) {
			log.warn("알림 payload가 null 입니다.");
			return;
		}

		String receiverId = (String) payload.get("receiverId");
		String senderId = (String) payload.get("senderId");
		String alarmCode = (String) payload.get("alarmCode");
		String customMessage = (String) payload.get("customMessage");
		String pk = (String) payload.get("pk");

		if (receiverId == null || receiverId.isBlank()) {
			log.warn("수신자 없는 알림은 전송하지 않습니다. alarmCode={}, senderId={}", alarmCode, senderId);
			return;
		}

		String template = DEFAULT_MESSAGE;
		String finalMessage = (customMessage != null && !customMessage.isBlank()) ? customMessage : DEFAULT_MESSAGE;
		String alarmCategory = null;
		String relatedUrl = DEFAULT_URL;

		if (alarmCode != null && !alarmCode.isBlank()) {
			try {
				AlarmTemplateVO alarmTemplate = alarmTemplateService.readAlarmTemplate(alarmCode);
				template = fallback(alarmTemplate.getAlarmMessage(), DEFAULT_MESSAGE);
				alarmCategory = alarmTemplate.getAlarmCategory();
				finalMessage = buildFinalMessage(alarmTemplate, senderId, pk);
				relatedUrl = buildRelatedUrl(alarmTemplate, senderId, pk);
			} catch (RuntimeException ex) {
				log.warn("알림 템플릿 조회 실패(alarmCode={}). 기본 메시지로 대체합니다.", alarmCode, ex);
			}
		}

		AlarmLogVO alarmLog = new AlarmLogVO();
		alarmLog.setReceiverId(receiverId);
		alarmLog.setSenderId(senderId);
		alarmLog.setAlarmCode(alarmCode);
		alarmLog.setAlarmMessage(finalMessage);
		alarmLog.setAlarmCategory(alarmCategory);
		alarmLog.setRelatedUrl(relatedUrl);

		try {
			alarmLogService.createAlarmLog(alarmLog);
		} catch (RuntimeException ex) {
			log.error("알림 로그 저장 실패(receiverId={}, alarmCode={})", receiverId, alarmCode, ex);
		}

		try {
			messagingTemplate.convertAndSend(
				"/topic/notify/" + receiverId,
				new NotificationTemplate(receiverId, senderId, alarmCode, template, finalMessage, alarmCategory, relatedUrl)
			);
		} catch (RuntimeException ex) {
			log.error("웹소켓 알림 전송 실패(receiverId={}, alarmCode={})", receiverId, alarmCode, ex);
		}
	}

	private String buildFinalMessage(AlarmTemplateVO alarmTemplate, String senderId, String pk) {
		String template = fallback(alarmTemplate.getAlarmMessage(), DEFAULT_MESSAGE);
		String alarmCode = alarmTemplate.getAlarmCode();
		if (alarmCode == null || alarmCode.isBlank()) {
			return template;
		}

		if (alarmCode.contains("APPROVAL")) {
			AuthorizationDocumentVO adVO = approvalMapper.selectAuthDocument(pk, senderId);
			return appendTitle(template, adVO != null ? adVO.getAtrzDocTtl() : null);
		}

		if (alarmCode.contains("PROJ")) {
			ProjectVO pVO = projMapper.selectProject(pk);
			return appendTitle(template, pVO != null ? pVO.getBizNm() : null);
		}

		if (alarmCode.contains("TASK")) {
			MainTaskVO mtVO = taskMapper.selectMainTask(pk);
			return appendTitle(template, mtVO != null ? mtVO.getTaskNm() : null);
		}

		if (alarmCode.contains("BOARD")) {
			BoardVO bVO = boardMapper.selectBoard(pk);
			return appendTitle(template, bVO != null ? bVO.getPstTtl() : null);
		}

		if (alarmCode.contains("MAIL")) {
			EmailContentVO emVO = emailmapper.selectEmailContent(pk);
			return appendTitle(template, emVO != null ? emVO.getSubject() : null);
		}

		return template;
	}

	private String buildRelatedUrl(AlarmTemplateVO alarmTemplate, String senderId, String pk) {
		String relatedUrl = fallback(alarmTemplate.getRelatedUrl(), DEFAULT_URL);
		String alarmCode = alarmTemplate.getAlarmCode();
		if (alarmCode == null || alarmCode.isBlank()) {
			return relatedUrl;
		}

		if (alarmCode.contains("APPROVAL")) {
			AuthorizationDocumentVO adVO = approvalMapper.selectAuthDocument(pk, senderId);
			return adVO != null ? relatedUrl + "/detail/" + adVO.getAtrzDocId() : relatedUrl;
		}

		if (alarmCode.contains("PROJ")) {
			ProjectVO pVO = projMapper.selectProject(pk);
			return pVO != null ? relatedUrl + "/" + pVO.getBizId() : relatedUrl;
		}

		if (alarmCode.contains("TASK")) {
			MainTaskVO mtVO = taskMapper.selectMainTask(pk);
			return mtVO != null ? relatedUrl + "/" + mtVO.getBizId() : relatedUrl;
		}

		if (alarmCode.contains("BOARD")) {
			BoardVO bVO = boardMapper.selectBoard(pk);
			return bVO != null ? relatedUrl + "/" + bVO.getPstId() : relatedUrl;
		}

		if (alarmCode.contains("MAIL")) {
			EmailContentVO emVO = emailmapper.selectEmailContent(pk);
			return emVO != null ? relatedUrl + "/detail/" + emVO.getEmailContId() : relatedUrl;
		}

		return relatedUrl;
	}

	private String appendTitle(String base, String title) {
		if (title == null || title.isBlank()) {
			return base;
		}
		return base + "<br/><p class=\"notification-subtitle text-sm\">" + title + "</p>";
	}

	private String fallback(String value, String defaultValue) {
		return (value == null || value.isBlank()) ? defaultValue : value;
	}
}
