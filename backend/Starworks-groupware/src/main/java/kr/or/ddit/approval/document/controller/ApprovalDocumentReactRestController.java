package kr.or.ddit.approval.document.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import kr.or.ddit.approval.document.dto.ApprovalActionRequest;
import kr.or.ddit.approval.document.dto.ApprovalCreateRequest;
import kr.or.ddit.approval.document.dto.ApprovalLineRequest;
import kr.or.ddit.approval.document.service.AuthorizationDocumentService;
import kr.or.ddit.approval.line.service.AuthorizationLineService;
import kr.or.ddit.approval.otp.service.OtpService;
import kr.or.ddit.approval.receiver.service.AuthorizationDocumentReceiverService;
import kr.or.ddit.approval.temp.service.AuthorizationTempService;
import kr.or.ddit.approval.template.service.AuthorizationDocumentTemplateService;
import kr.or.ddit.comm.file.service.FileDetailService;
import kr.or.ddit.mybatis.mapper.ApprovalReactMapper;
import kr.or.ddit.vo.AuthorizationDocumentReceiverVO;
import kr.or.ddit.vo.AuthorizationDocumentTemplateVO;
import kr.or.ddit.vo.AuthorizationDocumentVO;
import kr.or.ddit.vo.AuthorizationLineVO;
import kr.or.ddit.vo.AuthorizationTempVO;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/rest/approval-documents")
@RequiredArgsConstructor
public class ApprovalDocumentReactRestController {

    private static final int PAGE_SIZE = 12;
    private static final Set<String> FINAL_CODES = Set.of("A204", "A205", "A206");

    private final AuthorizationDocumentService service;
    private final AuthorizationLineService lineService;
    private final OtpService otpService;
    private final ApprovalReactMapper approvalReactMapper;
    private final AuthorizationDocumentReceiverService receiverService;
    private final AuthorizationDocumentTemplateService templateService;
    private final AuthorizationTempService tempService;
    private final FileDetailService fileDetailService;
    private final ObjectMapper objectMapper;

    @GetMapping("/summary")
    public ResponseEntity<?> readSummary(Authentication authentication) {
        String userId = authentication.getName();
        Map<String, Integer> counts = buildSummary(userId);
        return ResponseEntity.ok(Map.of(
            "approvalCount", counts.getOrDefault("inboxPending", 0),
            "counts", counts
        ));
    }

    @GetMapping
    public ResponseEntity<?> readDocuments(
        @RequestParam(name = "section", required = false) String section,
        @RequestParam(name = "status", required = false) String status,
        @RequestParam(name = "page", defaultValue = "1") int page,
        @RequestParam(name = "keyword", required = false) String keyword,
        @RequestParam(name = "templateId", required = false) String templateId,
        @RequestParam(name = "dateFrom", required = false) String dateFrom,
        @RequestParam(name = "dateTo", required = false) String dateTo,
        @RequestParam(name = "tab", required = false) String legacyTab,
        Authentication authentication
    ) {
        String resolvedSection = resolveSection(section, legacyTab);
        String resolvedStatus = resolveStatus(resolvedSection, status);
        String userId = authentication.getName();

        List<AuthorizationDocumentVO> items = resolveSectionDocuments(resolvedSection, userId);
        Map<String, Integer> statusCounts = computeStatusCounts(resolvedSection, items);
        List<AuthorizationDocumentVO> filtered = applyFilters(items, resolvedSection, resolvedStatus, keyword, templateId, dateFrom, dateTo);
        filtered.sort(Comparator.comparing(AuthorizationDocumentVO::getAtrzSbmtDt, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(AuthorizationDocumentVO::getAtrzDocId, Comparator.nullsLast(Comparator.reverseOrder())));

        int currentPage = Math.max(page, 1);
        int totalRecords = filtered.size();
        int totalPages = totalRecords == 0 ? 1 : (int) Math.ceil((double) totalRecords / PAGE_SIZE);
        if (currentPage > totalPages) {
            currentPage = totalPages;
        }

        int fromIndex = Math.max(0, (currentPage - 1) * PAGE_SIZE);
        int toIndex = Math.min(totalRecords, fromIndex + PAGE_SIZE);
        List<AuthorizationDocumentVO> pageItems = fromIndex >= totalRecords ? List.of() : filtered.subList(fromIndex, toIndex);

        return ResponseEntity.ok(Map.of(
            "section", resolvedSection,
            "status", resolvedStatus,
            "items", pageItems,
            "page", currentPage,
            "totalPages", totalPages,
            "totalRecords", totalRecords,
            "statusCounts", statusCounts
        ));
    }

    @GetMapping("/{atrzDocId:ATRZ[0-9A-Za-z]+}")
    public ResponseEntity<?> readDocument(
        @PathVariable String atrzDocId,
        Authentication authentication
    ) {
        if (!org.springframework.util.StringUtils.hasText(atrzDocId)) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "문서 ID가 비어 있습니다."
            ));
        }

        requireDocumentAccess(atrzDocId, authentication.getName());
        return ResponseEntity.ok(buildDocumentResponse(atrzDocId, authentication.getName()));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createDocumentJson(
        @RequestBody ApprovalCreateRequest request,
        Authentication authentication
    ) {
        return createDocumentInternal(request, List.of(), authentication);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createDocumentMultipart(
        @RequestPart("payload") String payload,
        @RequestPart(name = "files", required = false) List<MultipartFile> files,
        Authentication authentication
    ) throws IOException {
        ApprovalCreateRequest request = objectMapper.readValue(payload, ApprovalCreateRequest.class);
        return createDocumentInternal(request, files, authentication);
    }

    @PostMapping("/{atrzDocId}/approve")
    public ResponseEntity<?> approveDocument(
        @PathVariable String atrzDocId,
        @RequestBody(required = false) ApprovalActionRequest request,
        Authentication authentication
    ) {
        ApprovalActionRequest safeRequest = request == null ? new ApprovalActionRequest() : request;
        String loginId = authentication.getName();
        AuthorizationLineVO pendingLine = getActionableLine(atrzDocId, loginId);

        if (!isOtpValid(loginId, safeRequest.getOtpCode())) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "OTP 코드가 유효하지 않습니다."
            ));
        }

        if (lineService.readPreviousUnapprovedCount(atrzDocId, pendingLine.getAtrzLineSeq()) > 0) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "success", false,
                "message", "이전 결재선이 아직 처리되지 않았습니다."
            ));
        }

        lineService.modifyApproveAndUpdateStatus(
            atrzDocId,
            pendingLine.getAtrzLineSqn(),
            pendingLine.getAtrzLineSeq(),
            safeRequest.getOpinion(),
            safeRequest.getSignFileId(),
            safeRequest.getHtmlData()
        );

        return ResponseEntity.ok(Map.of(
            "success", true,
            "detail", buildDocumentResponse(atrzDocId, loginId)
        ));
    }

    @PostMapping("/{atrzDocId}/reject")
    public ResponseEntity<?> rejectDocument(
        @PathVariable String atrzDocId,
        @RequestBody(required = false) ApprovalActionRequest request,
        Authentication authentication
    ) {
        ApprovalActionRequest safeRequest = request == null ? new ApprovalActionRequest() : request;
        String loginId = authentication.getName();
        AuthorizationLineVO pendingLine = getActionableLine(atrzDocId, loginId);

        if (!org.springframework.util.StringUtils.hasText(safeRequest.getOpinion())) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "반려 사유를 입력해야 합니다."
            ));
        }

        if (!isOtpValid(loginId, safeRequest.getOtpCode())) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "OTP 코드가 유효하지 않습니다."
            ));
        }

        if (lineService.readPreviousUnapprovedCount(atrzDocId, pendingLine.getAtrzLineSeq()) > 0) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "success", false,
                "message", "이전 결재선이 아직 처리되지 않았습니다."
            ));
        }

        lineService.processRejection(atrzDocId, pendingLine.getAtrzLineSqn(), safeRequest.getOpinion());

        return ResponseEntity.ok(Map.of(
            "success", true,
            "detail", buildDocumentResponse(atrzDocId, loginId)
        ));
    }

    @PostMapping("/{atrzDocId}/retract")
    public ResponseEntity<?> retractDocument(
        @PathVariable String atrzDocId,
        Authentication authentication
    ) {
        AuthorizationDocumentVO document = service.readAuthDocument(atrzDocId, authentication.getName());
        if (!Objects.equals(document.getAtrzUserId(), authentication.getName())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "success", false,
                "message", "기안자만 문서를 회수할 수 있습니다."
            ));
        }

        if (!List.of("A202", "A203").contains(document.getCrntAtrzStepCd())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "success", false,
                "message", "현재 상태에서는 문서를 회수할 수 없습니다."
            ));
        }

        service.updateDocumentStatus(atrzDocId, "A205");

        return ResponseEntity.ok(Map.of(
            "success", true,
            "detail", buildDocumentResponse(atrzDocId, authentication.getName())
        ));
    }

    private ResponseEntity<?> createDocumentInternal(
        ApprovalCreateRequest request,
        List<MultipartFile> files,
        Authentication authentication
    ) {
        if (!org.springframework.util.StringUtils.hasText(request.getAtrzDocTmplId())
            || !org.springframework.util.StringUtils.hasText(request.getAtrzDocTtl())
            || !org.springframework.util.StringUtils.hasText(request.getHtmlData())) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "양식, 제목, 본문은 필수입니다."
            ));
        }

        List<AuthorizationLineVO> approvalLines = toApprovalLines(request.getApprovalLines());
        if (approvalLines.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "결재선을 한 명 이상 선택해야 합니다."
            ));
        }

        AuthorizationDocumentVO document = new AuthorizationDocumentVO();
        document.setAtrzDocId(service.getNextDocId());
        document.setAtrzUserId(authentication.getName());
        document.setAtrzDocTmplId(request.getAtrzDocTmplId());
        document.setAtrzDocTtl(request.getAtrzDocTtl().trim());
        document.setHtmlData(request.getHtmlData());
        document.setOpenYn(org.springframework.util.StringUtils.hasText(request.getOpenYn()) ? request.getOpenYn() : "N");
        document.setApprovalLines(approvalLines);
        if (!CollectionUtils.isEmpty(files)) {
            document.setFileList(files);
        } else if (org.springframework.util.StringUtils.hasText(request.getAtrzFileId())) {
            document.setAtrzFileId(request.getAtrzFileId());
        }

        service.insertAuthDocument(document);
        insertReceivers(document.getAtrzDocId(), request.getReceiverIds());
        removeTempIfNeeded(request.getAtrzTempSqn(), authentication.getName());

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "success", true,
            "atrzDocId", document.getAtrzDocId(),
            "detail", buildDocumentResponse(document.getAtrzDocId(), authentication.getName())
        ));
    }

    private void removeTempIfNeeded(String atrzTempSqn, String userId) {
        if (!org.springframework.util.StringUtils.hasText(atrzTempSqn)) {
            return;
        }

        AuthorizationTempVO temp = tempService.readAuthTemp(atrzTempSqn);
        if (temp != null && Objects.equals(temp.getAtrzUserId(), userId)) {
            tempService.deleteAuthTemp(atrzTempSqn);
        }
    }

    private void insertReceivers(String atrzDocId, List<String> receiverIds) {
        if (CollectionUtils.isEmpty(receiverIds)) {
            return;
        }

        Set<String> deduplicated = receiverIds.stream()
            .filter(org.springframework.util.StringUtils::hasText)
            .map(String::trim)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        for (String receiverId : deduplicated) {
            AuthorizationDocumentReceiverVO receiver = new AuthorizationDocumentReceiverVO();
            receiver.setAtrzDocId(atrzDocId);
            receiver.setAtrzRcvrId(receiverId);
            receiver.setOpenYn("N");
            receiverService.createAuthDocumentReceiver(receiver);
        }
    }

    private AuthorizationLineVO getActionableLine(String atrzDocId, String loginId) {
        AuthorizationLineVO pendingLine = lineService.readPendingLineForUser(atrzDocId, loginId);
        if (pendingLine == null) {
            throw new IllegalStateException("처리 가능한 결재선이 없습니다.");
        }

        if ("A301".equals(pendingLine.getAtrzApprStts())) {
            lineService.markAsRead(atrzDocId, pendingLine.getAtrzLineSqn());
            pendingLine = lineService.readPendingLineForUser(atrzDocId, loginId);
        }

        if (pendingLine == null || !"A302".equals(pendingLine.getAtrzApprStts())) {
            throw new IllegalStateException("결재 처리 가능한 상태가 아닙니다.");
        }

        return pendingLine;
    }

    private Map<String, Object> buildDocumentResponse(String atrzDocId, String loginId) {
        AuthorizationDocumentVO document = service.readAuthDocument(atrzDocId, loginId);
        AuthorizationLineVO pendingLine = lineService.readPendingLineForUser(atrzDocId, loginId);

        if (pendingLine != null && "A301".equals(pendingLine.getAtrzApprStts())) {
            lineService.markAsRead(atrzDocId, pendingLine.getAtrzLineSqn());
            document = service.readAuthDocument(atrzDocId, loginId);
            pendingLine = lineService.readPendingLineForUser(atrzDocId, loginId);
        }

        boolean canApprove = pendingLine != null
            && "A302".equals(pendingLine.getAtrzApprStts())
            && lineService.readPreviousUnapprovedCount(atrzDocId, pendingLine.getAtrzLineSeq()) == 0;

        AuthorizationDocumentTemplateVO template = templateService.readAuthDocumentTemplate(document.getAtrzDocTmplId());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("document", document);
        response.put("template", template);
        response.put("pendingLine", pendingLine);
        response.put("canApprove", canApprove);
        response.put("canReject", canApprove);
        response.put("canRetract", Objects.equals(document.getAtrzUserId(), loginId) && List.of("A202", "A203").contains(document.getCrntAtrzStepCd()));
        response.put("currentSeq", pendingLine != null ? pendingLine.getAtrzLineSeq() : null);
        response.put("lineSqn", pendingLine != null ? pendingLine.getAtrzLineSqn() : null);
        response.put("receivers", receiverService.readAuthDocumentReceiverList(atrzDocId));
        response.put("attachments", org.springframework.util.StringUtils.hasText(document.getAtrzFileId())
            ? fileDetailService.readFileDetailList(document.getAtrzFileId())
            : List.of());
        return response;
    }

    private String resolveSection(String section, String legacyTab) {
        if (org.springframework.util.StringUtils.hasText(section)) {
            return section.trim().toLowerCase(Locale.ROOT);
        }
        if (!org.springframework.util.StringUtils.hasText(legacyTab)) {
            return "draft";
        }
        return switch (legacyTab.trim().toLowerCase(Locale.ROOT)) {
            case "inbox" -> "inbox";
            case "processed" -> "archive";
            case "draft" -> "draft";
            default -> "draft";
        };
    }

    private String resolveStatus(String section, String status) {
        if (org.springframework.util.StringUtils.hasText(status)) {
            return status.trim().toLowerCase(Locale.ROOT);
        }
        if ("draft".equals(section)) {
            return "progress";
        }
        return "all";
    }

    private List<AuthorizationDocumentVO> resolveSectionDocuments(String section, String userId) {
        return switch (section) {
            case "draft" -> approvalReactMapper.selectDraftDocuments(userId);
            case "inbox" -> approvalReactMapper.selectInboxDocuments(userId);
            case "upcoming" -> approvalReactMapper.selectUpcomingDocuments(userId);
            case "reference" -> approvalReactMapper.selectReferenceDocuments(userId);
            case "archive" -> approvalReactMapper.selectArchiveDocuments(userId);
            default -> throw new IllegalArgumentException("지원하지 않는 전자결재 섹션입니다.");
        };
    }

    private List<AuthorizationDocumentVO> applyFilters(
        List<AuthorizationDocumentVO> items,
        String section,
        String status,
        String keyword,
        String templateId,
        String dateFrom,
        String dateTo
    ) {
        LocalDate from = parseDate(dateFrom);
        LocalDate to = parseDate(dateTo);
        String loweredKeyword = org.springframework.util.StringUtils.hasText(keyword)
            ? keyword.trim().toLowerCase(Locale.ROOT)
            : null;

        return items.stream()
            .filter(item -> matchesStatus(section, status, item))
            .filter(item -> !org.springframework.util.StringUtils.hasText(templateId)
                || Objects.equals(item.getAtrzDocTmplId(), templateId))
            .filter(item -> matchesKeyword(item, loweredKeyword))
            .filter(item -> matchesDate(item, from, to))
            .collect(Collectors.toList());
    }

    private boolean matchesStatus(String section, String status, AuthorizationDocumentVO item) {
        if (!org.springframework.util.StringUtils.hasText(status) || "all".equals(status)) {
            return true;
        }
        String code = Objects.toString(item.getCrntAtrzStepCd(), "");
        if ("draft".equals(section)) {
            return switch (status) {
                case "progress" -> List.of("A202", "A203").contains(code);
                case "approved" -> "A206".equals(code);
                case "rejected" -> "A204".equals(code);
                case "retracted" -> "A205".equals(code);
                default -> true;
            };
        }
        if ("archive".equals(section)) {
            return switch (status) {
                case "approved" -> "A206".equals(code);
                case "rejected" -> "A204".equals(code);
                case "retracted" -> "A205".equals(code);
                default -> true;
            };
        }
        return true;
    }

    private boolean matchesKeyword(AuthorizationDocumentVO item, String keyword) {
        if (!org.springframework.util.StringUtils.hasText(keyword)) {
            return true;
        }
        String title = Objects.toString(item.getAtrzDocTtl(), "").toLowerCase(Locale.ROOT);
        String drafter = Objects.toString(item.getDrafterName(), "").toLowerCase(Locale.ROOT);
        String template = Objects.toString(item.getAtrzDocTmplNm(), "").toLowerCase(Locale.ROOT);
        return title.contains(keyword) || drafter.contains(keyword) || template.contains(keyword);
    }

    private boolean matchesDate(AuthorizationDocumentVO item, LocalDate from, LocalDate to) {
        if (item.getAtrzSbmtDt() == null) {
            return from == null && to == null;
        }
        LocalDate submitted = item.getAtrzSbmtDt().toLocalDate();
        if (from != null && submitted.isBefore(from)) {
            return false;
        }
        if (to != null && submitted.isAfter(to)) {
            return false;
        }
        return true;
    }

    private Map<String, Integer> buildSummary(String userId) {
        List<AuthorizationDocumentVO> drafts = approvalReactMapper.selectDraftDocuments(userId);
        List<AuthorizationDocumentVO> inbox = approvalReactMapper.selectInboxDocuments(userId);
        List<AuthorizationDocumentVO> upcoming = approvalReactMapper.selectUpcomingDocuments(userId);
        List<AuthorizationDocumentVO> reference = approvalReactMapper.selectReferenceDocuments(userId);
        List<AuthorizationDocumentVO> archive = approvalReactMapper.selectArchiveDocuments(userId);
        List<AuthorizationTempVO> temps = tempService.readAuthTempList();

        int draftProgress = (int) drafts.stream()
            .filter(item -> List.of("A202", "A203").contains(item.getCrntAtrzStepCd()))
            .count();

        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("draftProgress", draftProgress);
        counts.put("inboxPending", inbox.size());
        counts.put("upcoming", upcoming.size());
        counts.put("reference", reference.size());
        counts.put("tempSaved", temps.size());
        counts.put("archive", archive.size());
        counts.put("draftApproved", (int) drafts.stream().filter(item -> "A206".equals(item.getCrntAtrzStepCd())).count());
        counts.put("draftRejected", (int) drafts.stream().filter(item -> "A204".equals(item.getCrntAtrzStepCd())).count());
        counts.put("draftRetracted", (int) drafts.stream().filter(item -> "A205".equals(item.getCrntAtrzStepCd())).count());
        return counts;
    }

    private Map<String, Integer> computeStatusCounts(String section, List<AuthorizationDocumentVO> items) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("all", items.size());
        if ("draft".equals(section) || "archive".equals(section)) {
            counts.put("progress", (int) items.stream().filter(item -> List.of("A202", "A203").contains(item.getCrntAtrzStepCd())).count());
            counts.put("approved", (int) items.stream().filter(item -> "A206".equals(item.getCrntAtrzStepCd())).count());
            counts.put("rejected", (int) items.stream().filter(item -> "A204".equals(item.getCrntAtrzStepCd())).count());
            counts.put("retracted", (int) items.stream().filter(item -> "A205".equals(item.getCrntAtrzStepCd())).count());
        }
        return counts;
    }

    private LocalDate parseDate(String value) {
        if (!org.springframework.util.StringUtils.hasText(value)) {
            return null;
        }
        return LocalDate.parse(value.trim());
    }

    private List<AuthorizationLineVO> toApprovalLines(List<ApprovalLineRequest> requests) {
        if (requests == null) {
            return List.of();
        }

        List<AuthorizationLineVO> lines = new ArrayList<>();
        Set<String> seenUserIds = new LinkedHashSet<>();

        for (ApprovalLineRequest request : requests) {
            if (request == null || !org.springframework.util.StringUtils.hasText(request.getAtrzApprUserId())) {
                continue;
            }

            String userId = request.getAtrzApprUserId().trim();
            if (seenUserIds.contains(userId)) {
                continue;
            }

            seenUserIds.add(userId);

            AuthorizationLineVO line = new AuthorizationLineVO();
            line.setAtrzApprUserId(userId);
            line.setApprAtrzYn(org.springframework.util.StringUtils.hasText(request.getApprAtrzYn()) ? request.getApprAtrzYn() : "N");
            lines.add(line);
        }

        return lines;
    }

    private boolean isOtpValid(String userId, Integer otpCode) {
        String secretKey = otpService.getUserOtpSecret(userId);
        if (StringUtils.isNotBlank(secretKey)) {
            return otpCode != null && otpService.validateOtp(secretKey, otpCode);
        }
        return true;
    }

    private void requireDocumentAccess(String atrzDocId, String userId) {
        if (approvalReactMapper.existsDocumentAccess(atrzDocId, userId) > 0) {
            return;
        }
        throw new AccessDeniedException("문서에 접근할 수 없습니다.");
    }
}
