package kr.or.ddit.approval.line.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.businesstrip.service.BusinessTripService;
import kr.or.ddit.comm.file.service.impl.FileUploadServiceImpl;
import kr.or.ddit.menu.atrz.service.NewMenuAtrzService;
import kr.or.ddit.mybatis.mapper.AuthorizationDocumentMapper;
import kr.or.ddit.mybatis.mapper.AuthorizationDocumentTemplateMapper;
import kr.or.ddit.mybatis.mapper.AuthorizationLineMapper;
import kr.or.ddit.mybatis.mapper.VactionMapper;
import kr.or.ddit.vo.AuthorizationDocumentTemplateVO;
import kr.or.ddit.vo.AuthorizationDocumentVO;
import kr.or.ddit.vo.AuthorizationLineVO;
import kr.or.ddit.vo.BusinessTripVO;
import kr.or.ddit.vo.NewMenuAtrzVO;
import kr.or.ddit.vo.VactionVO;
import kr.or.ddit.websocket.service.impl.NotificationServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorizationLineServiceImpl implements AuthorizationLineService {

    private final AuthorizationLineMapper mapper;
    private final AuthorizationDocumentMapper docmapper;
    private final AuthorizationDocumentTemplateMapper templateMapper;
    private final NotificationServiceImpl notificationService;
    private final NewMenuAtrzService newMenuAtrzService;
    private final VactionMapper vactionMapper;
    private final BusinessTripService businessTripService;
    private final FileUploadServiceImpl fileUploadService;

    @Override
    public boolean createAuthorizationLine(AuthorizationLineVO authLine) {
        return mapper.insertAuthLine(authLine) > 0;
    }

    @Override
    public List<AuthorizationLineVO> readAuthorizationLineList(String atrzDocId) {
        return mapper.selectAuthorizationLineList(atrzDocId);
    }

    @Override
    public AuthorizationLineVO readPendingLineForUser(String docId, String userId) {
        return mapper.selectPendingLineForUser(docId, userId);
    }

    @Override
    public int readPreviousUnapprovedCount(String docId, int lineSeq) {
        return mapper.selectPreviousUnapprovedCount(docId, lineSeq);
    }

    @Override
    @Transactional
    public boolean modifyApproveLine(String docId, int lineSqn, String opinion, String signFileId) {
        int updated = mapper.updateApproveLine(docId, lineSqn, opinion, signFileId);
        return updated == 1;
    }

    @Override
    public boolean readHasNextPending(String docId, int currentSeq) {
        return mapper.existsNextPending(docId, currentSeq) > 0;
    }

    @Override
    public void modifyDocumentStatus(String docId, String stepCode) {
        mapper.updateDocumentStatus(docId, stepCode);
    }

    @Override
    @Transactional
    public String modifyApproveAndUpdateStatus(String docId, int lineSqn, int currentSeq, String opinion, String signFileId, String htmlData) {
        int updated = mapper.updateApproveLine(docId, lineSqn, opinion, signFileId);
        if (updated != 1) {
            throw new IllegalStateException("이미 처리되었거나 승인할 수 없는 결재선입니다.");
        }

        updateDocumentHtml(docId, htmlData);

        AuthorizationLineVO nextLine = mapper.selectNextLineBySeq(docId, currentSeq);
        if (nextLine != null) {
            if (nextLine.getAtrzApprStts() == null) {
                mapper.updateLineStatusToUnread(nextLine.getAtrzDocId(), nextLine.getAtrzLineSqn());
                notifyApprovalRequested(nextLine.getAtrzApprUserId(), docId);
            }
            mapper.updateDocumentStatus(docId, "A203");
            return "A203";
        }

        AuthorizationDocumentVO document = docmapper.selectAuthDocument(docId, null);
        mapper.updateDocumentStatus(docId, "A206");
        autoInsertNewMenuFromHtml(document);
        autoInsertVacationFromHtml(document);
        autoInsertBusinessTripFromHtml(document);
        notifyApprovalCompleted(document.getAtrzUserId(), docId);
        saveApprovalPdf(docId);
        return "A206";
    }

    @Override
    @Transactional
    public void delegateApproval(AuthorizationLineVO line, String htmlData) {
        if (line == null
            || line.getAtrzDocId() == null
            || line.getAtrzApprUserId() == null
            || line.getAtrzLineSqn() == null) {
            throw new IllegalArgumentException("전결 처리 파라미터가 올바르지 않습니다.");
        }

        int updatedSelf = mapper.updateLineForDelegation(line);
        if (updatedSelf == 0) {
            throw new IllegalStateException("전결 처리할 수 있는 결재선이 없습니다.");
        }

        updateDocumentHtml(line.getAtrzDocId(), htmlData);
        mapper.updateSubsequentLinesAsDelegated(line.getAtrzDocId(), line.getAtrzLineSqn());
        docmapper.updateDocumentStatus(line.getAtrzDocId(), "A206");

        AuthorizationDocumentVO document = docmapper.selectAuthDocument(line.getAtrzDocId(), null);
        autoInsertNewMenuFromHtml(document);
        autoInsertVacationFromHtml(document);
        autoInsertBusinessTripFromHtml(document);
        notifyApprovalCompleted(document.getAtrzUserId(), document.getAtrzDocId());
        saveApprovalPdf(line.getAtrzDocId());
    }

    @Override
    public boolean markAsRead(String docId, int lineSqn) {
        int updated = mapper.updateLineStatusToUnprocessed(docId, lineSqn);
        return updated == 1;
    }

    @Override
    @Transactional
    public void processRejection(String docId, int lineSqn, String opinion) {
        int updated = mapper.updateRejectLine(docId, lineSqn, opinion);
        if (updated != 1) {
            throw new IllegalStateException("이미 처리되었거나 반려할 수 없는 결재선입니다.");
        }

        AuthorizationDocumentVO document = docmapper.selectAuthDocument(docId, null);
        notifyApprovalRejected(document.getAtrzUserId(), docId);
        docmapper.updateDocumentStatus(docId, "A204");
    }

    private void updateDocumentHtml(String docId, String htmlData) {
        AuthorizationDocumentVO authorizationDocument = new AuthorizationDocumentVO();
        authorizationDocument.setAtrzDocId(docId);
        authorizationDocument.setHtmlData(htmlData);
        docmapper.updateAuthorizationDocument(authorizationDocument);
    }

    private void notifyApprovalRequested(String receiverId, String docId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("receiverId", receiverId);
        payload.put("senderId", "system");
        payload.put("alarmCode", "APPROVAL_01");
        payload.put("pk", docId);
        notificationService.sendNotification(payload);
    }

    private void notifyApprovalCompleted(String receiverId, String docId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("receiverId", receiverId);
        payload.put("senderId", "system");
        payload.put("alarmCode", "APPROVAL_02");
        payload.put("pk", docId);
        notificationService.sendNotification(payload);
    }

    private void notifyApprovalRejected(String receiverId, String docId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("receiverId", receiverId);
        payload.put("senderId", "system");
        payload.put("alarmCode", "APPROVAL_03");
        payload.put("pk", docId);
        notificationService.sendNotification(payload);
    }

    private void saveApprovalPdf(String docId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String loginId = authentication != null ? authentication.getName() : null;
            AuthorizationDocumentVO adVO = docmapper.selectAuthDocument(docId, loginId);
            fileUploadService.savePdfS3(adVO);
        } catch (Exception e) {
            log.warn("[결재 PDF 저장 생략] 문서ID={}, 사유={}", docId, e.getMessage());
        }
    }

    private void autoInsertNewMenuFromHtml(AuthorizationDocumentVO doc) {
        try {
            if (!matchesTemplate(doc, "ATRZDOC104", "NEW_MENU")) {
                return;
            }

            String html = doc.getHtmlData();
            NewMenuAtrzVO newMenu = new NewMenuAtrzVO();
            newMenu.setAtrzDocId(doc.getAtrzDocId());
            newMenu.setMenuNm(extractFieldValue(html, "메뉴명"));
            newMenu.setCategoryNm(extractFieldValue(html, "카테고리"));
            newMenu.setStandardCd(extractFieldValue(html, "규격"));
            newMenu.setIngredientContent(extractFieldValue(html, "재료 및 레시피"));
            newMenu.setMarketingContent(extractFieldValue(html, "마케팅 및 운영 계획"));

            String releaseYmd = extractFieldValue(html, "출시 예정일");
            if (!releaseYmd.isBlank()) {
                newMenu.setReleaseYmd(LocalDate.parse(releaseYmd.trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            }

            String price = extractFieldValue(html, "판매가").replaceAll("[^0-9]", "");
            if (!price.isBlank()) {
                newMenu.setPriceAmt(Integer.parseInt(price));
            }

            String ratio = extractFieldValue(html, "원가율").replaceAll("[^0-9]", "");
            if (!ratio.isBlank()) {
                newMenu.setCostRatioAmt(Integer.parseInt(ratio));
            }

            newMenuAtrzService.createNewMenuAtrz(newMenu);
        } catch (Exception e) {
            log.error("[신메뉴 자동등록 실패] 문서ID={}, 예외={}", doc.getAtrzDocId(), e.getMessage());
        }
    }

    private void autoInsertVacationFromHtml(AuthorizationDocumentVO doc) {
        try {
            if (!matchesTemplate(doc, "ATRZDOC101", "ATRZDOC001", "VAC_REQ")) {
                return;
            }

            String html = doc.getHtmlData();
            String[] period = extractPeriod(html, "기간");
            if (!hasPeriod(period)) {
                return;
            }

            VactionVO vac = new VactionVO();
            vac.setAtrzDocId(doc.getAtrzDocId());
            vac.setVactUserId(doc.getAtrzUserId());
            vac.setVactCd(mapVacationCode(extractFieldValue(html, "휴가 종류")));
            vac.setVactBgngDt(LocalDateTime.of(LocalDate.parse(period[0]), LocalTime.of(9, 0)));
            vac.setVactEndDt(LocalDateTime.of(LocalDate.parse(period[1]), LocalTime.of(18, 0)));
            vac.setUseVactCnt((int) Math.max(1, ChronoUnit.DAYS.between(vac.getVactBgngDt().toLocalDate(), vac.getVactEndDt().toLocalDate()) + 1));
            vac.setVactExpln(extractFieldValue(html, "사유"));
            vac.setAllday("N");
            vactionMapper.insertVaction(vac);
        } catch (Exception e) {
            log.error("[휴가 자동등록 실패] 문서ID={}, 예외={}", doc.getAtrzDocId(), e.getMessage());
        }
    }

    private void autoInsertBusinessTripFromHtml(AuthorizationDocumentVO doc) {
        try {
            if (!matchesTemplate(doc, "ATRZDOC002", "TRIP_REQ")) {
                return;
            }

            String[] period = extractPeriod(doc.getHtmlData(), "기간");
            if (!hasPeriod(period)) {
                return;
            }

            BusinessTripVO trip = new BusinessTripVO();
            trip.setBztrSqn(Math.abs(Objects.hash(doc.getAtrzDocId(), doc.getAtrzUserId())));
            trip.setAtrzDocId(doc.getAtrzDocId());
            trip.setBztrUserId(doc.getAtrzUserId());
            trip.setBztrCd("TRIP");
            trip.setBztrBgngDt(LocalDateTime.of(LocalDate.parse(period[0]), LocalTime.of(9, 0)));
            trip.setBztrEndDt(LocalDateTime.of(LocalDate.parse(period[1]), LocalTime.of(18, 0)));
            trip.setBztrExpln(extractFieldValue(doc.getHtmlData(), "목적"));
            trip.setAllday("N");
            businessTripService.createBusinessTrip(trip);
        } catch (Exception e) {
            log.error("[출장 자동등록 실패] 문서ID={}, 예외={}", doc.getAtrzDocId(), e.getMessage());
        }
    }

    private boolean matchesTemplate(AuthorizationDocumentVO doc, String... targets) {
        if (doc == null || doc.getAtrzDocTmplId() == null) {
            return false;
        }

        for (String target : targets) {
            if (target.equalsIgnoreCase(doc.getAtrzDocTmplId())) {
                return true;
            }
        }

        AuthorizationDocumentTemplateVO template = templateMapper.selectAuthDocumentTemplate(doc.getAtrzDocTmplId());
        if (template == null || template.getAtrzDocCd() == null) {
            return false;
        }

        for (String target : targets) {
            if (target.equalsIgnoreCase(template.getAtrzDocCd())) {
                return true;
            }
        }

        return false;
    }

    private String mapVacationCode(String name) {
        if (name == null || name.isBlank()) {
            return "E101";
        }

        String value = name.trim();
        if (value.contains("반차")) {
            return "E102";
        }
        if (value.contains("공가")) {
            return "E103";
        }
        if (value.contains("무급")) {
            return "E104";
        }
        if (value.contains("병가")) {
            return "E105";
        }
        if (value.contains("경조")) {
            return "E106";
        }
        return "E101";
    }

    private boolean hasPeriod(String[] period) {
        return period != null
            && period.length == 2
            && !period[0].isBlank()
            && !period[1].isBlank();
    }

    private String extractFieldValue(String html, String label) {
        String tableValue = extractTableCellValue(html, label);
        if (!tableValue.isBlank()) {
            return tableValue;
        }

        Pattern inputPattern = Pattern.compile(Pattern.quote(label) + ".*?<input[^>]*value=\"([^\"]*)\"[^>]*>", Pattern.DOTALL);
        Matcher inputMatcher = inputPattern.matcher(html == null ? "" : html);
        if (inputMatcher.find()) {
            return inputMatcher.group(1).trim();
        }

        Pattern spanPattern = Pattern.compile(Pattern.quote(label) + ".*?<span[^>]*class=\"changeInput\"[^>]*>(.*?)</span>", Pattern.DOTALL);
        Matcher spanMatcher = spanPattern.matcher(html == null ? "" : html);
        if (spanMatcher.find()) {
            return stripTags(spanMatcher.group(1));
        }

        return "";
    }

    private String extractTableCellValue(String html, String label) {
        if (html == null) {
            return "";
        }

        Pattern pattern = Pattern.compile(
            "<th[^>]*>\\s*" + Pattern.quote(label) + "\\s*</th>\\s*<td[^>]*>(.*?)</td>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        Matcher matcher = pattern.matcher(html);
        if (!matcher.find()) {
            return "";
        }
        return stripTags(matcher.group(1));
    }

    private String[] extractPeriod(String html, String label) {
        String value = extractFieldValue(html, label);
        Matcher matcher = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})").matcher(value);
        String start = "";
        String end = "";
        if (matcher.find()) {
            start = matcher.group(1);
        }
        if (matcher.find()) {
            end = matcher.group(1);
        }
        if (end.isBlank()) {
            end = start;
        }
        return new String[] {start, end};
    }

    private String stripTags(String value) {
        return value == null ? "" : value.replaceAll("<br\\s*/?>", "\n").replaceAll("<[^>]+>", "").trim();
    }
}
