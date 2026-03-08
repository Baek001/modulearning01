package kr.or.ddit.contract.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import kr.or.ddit.comm.file.FileFolderType;
import kr.or.ddit.comm.file.service.FileDetailService;
import kr.or.ddit.comm.file.service.impl.FileUploadServiceImpl;
import kr.or.ddit.comm.pdf.PdfServiceImpl;
import kr.or.ddit.vo.FileDetailVO;
import kr.or.ddit.websocket.service.impl.NotificationServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContractService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<Object>> LIST_TYPE = new TypeReference<>() {};
    private static final Set<String> PENDING_SIGNER_STATUSES = Set.of("pending", "opened");
    private static final Set<String> FINAL_SIGNER_STATUSES = Set.of("signed", "declined", "expired", "skipped");

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${cloud.aws.s3.bucket:}")
    private String bucket;

    @Value("${file-info.storage-mode:s3}")
    private String storageMode;

    private final NamedParameterJdbcTemplate namedJdbc;
    private final ObjectMapper objectMapper;
    private final FileUploadServiceImpl fileUploadService;
    private final FileDetailService fileDetailService;
    private final PdfServiceImpl pdfService;
    private final NotificationServiceImpl notificationService;
    private final S3Client s3Client;

    public record FileDownloadPayload(String fileName, String contentType, byte[] bytes) {}

    private static final class InvitationContext {
        private final Long contractId;
        private final Long signerId;
        private final Long invitationId;
        private final Map<String, Object> contract;
        private final Map<String, Object> signer;
        private final Map<String, Object> invitation;

        private InvitationContext(
            Long contractId,
            Long signerId,
            Long invitationId,
            Map<String, Object> contract,
            Map<String, Object> signer,
            Map<String, Object> invitation
        ) {
            this.contractId = contractId;
            this.signerId = signerId;
            this.invitationId = invitationId;
            this.contract = contract;
            this.signer = signer;
            this.invitation = invitation;
        }
    }

    private MapSqlParameterSource accessParams(String userId, boolean admin) {
        return new MapSqlParameterSource().addValue("userId", userId).addValue("admin", admin);
    }

    private String contractAccessWhere() {
        return "(:admin = TRUE OR D.CREATOR_USER_ID = :userId OR EXISTS (SELECT 1 FROM CONTRACT_SIGNER S WHERE S.CONTRACT_ID = D.CONTRACT_ID AND S.INTERNAL_USER_ID = :userId))";
    }

    private void requireAdmin(boolean admin) {
        if (!admin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrator access is required.");
        }
    }

    private int queryCount(String sql, MapSqlParameterSource params) {
        Integer count = namedJdbc.queryForObject(sql, params, Integer.class);
        return count == null ? 0 : count;
    }

    private int queryCount(String sql, Map<String, ?> params) {
        Integer count = namedJdbc.queryForObject(sql, params, Integer.class);
        return count == null ? 0 : count;
    }

    private String stringValue(Object value) {
        return stringValue(value, null);
    }

    private String stringValue(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value);
        return text.isBlank() ? defaultValue : text;
    }

    private String blankToNull(Object value) {
        String text = stringValue(value);
        return StringUtils.isBlank(text) ? null : text;
    }

    private Integer intValue(Object value, int defaultValue) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private boolean truthy(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Set.of("true", "y", "yes", "1", "on").contains(String.valueOf(value).trim().toLowerCase(Locale.ROOT));
    }

    private LocalDateTime localDateTime(Object value) {
        if (value instanceof LocalDateTime dateTime) {
            return dateTime;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(String.valueOf(value));
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private String requiredText(Object value, String message) {
        String text = stringValue(value);
        if (StringUtils.isBlank(text)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return text.trim();
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String text) {
            return text;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid JSON payload.", ex);
        }
    }

    private Object parseJson(String raw) {
        if (StringUtils.isBlank(raw)) {
            return null;
        }
        try {
            if (raw.trim().startsWith("{")) {
                return objectMapper.readValue(raw, MAP_TYPE);
            }
            if (raw.trim().startsWith("[")) {
                return objectMapper.readValue(raw, LIST_TYPE);
            }
            return raw;
        } catch (JsonProcessingException ex) {
            return raw;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> asList(Object value) {
        if (value instanceof List<?> list) {
            return (List<T>) list;
        }
        return new ArrayList<>();
    }

    private String trimTrailingSlash(String value) {
        if (StringUtils.isBlank(value)) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    @Transactional
    public Map<String, Object> readDashboard(String userId, boolean admin) {
        expireOutdatedContracts();
        MapSqlParameterSource params = accessParams(userId, admin);
        String whereClause = contractAccessWhere();

        Map<String, Object> counts = new LinkedHashMap<>();
        counts.put("draft", queryCount("SELECT COUNT(*) FROM CONTRACT_DOCUMENT D WHERE " + whereClause + " AND D.STATUS_CD IN ('draft', 'ready')", params));
        counts.put("active", queryCount("SELECT COUNT(*) FROM CONTRACT_DOCUMENT D WHERE " + whereClause + " AND D.STATUS_CD IN ('sent', 'in_progress')", params));
        counts.put("completed", queryCount("SELECT COUNT(*) FROM CONTRACT_DOCUMENT D WHERE " + whereClause + " AND D.STATUS_CD = 'completed'", params));
        counts.put("expired", queryCount("SELECT COUNT(*) FROM CONTRACT_DOCUMENT D WHERE " + whereClause + " AND D.STATUS_CD = 'expired'", params));
        counts.put("cancelled", queryCount("SELECT COUNT(*) FROM CONTRACT_DOCUMENT D WHERE " + whereClause + " AND D.STATUS_CD = 'cancelled'", params));
        counts.put("templates", queryCount("SELECT COUNT(*) FROM CONTRACT_TEMPLATE WHERE COALESCE(ACTIVE_YN, 'Y') = 'Y'", Map.of()));
        counts.put("requests", queryCount(
            admin
                ? "SELECT COUNT(*) FROM CONTRACT_TEMPLATE_REQUEST WHERE REQUEST_STATUS_CD IN ('requested', 'reviewing')"
                : "SELECT COUNT(*) FROM CONTRACT_TEMPLATE_REQUEST WHERE REQUESTER_USER_ID = :userId",
            admin ? Map.of() : Map.of("userId", userId)
        ));

        List<Map<String, Object>> recentContracts = namedJdbc.queryForList(
            "SELECT D.CONTRACT_ID AS contractId, D.CONTRACT_REF AS contractRef, D.CONTRACT_TITLE AS contractTitle, D.STATUS_CD AS statusCd, " +
                "D.SEND_TYPE_CD AS sendTypeCd, D.SIGNING_FLOW_CD AS signingFlowCd, D.CRT_DT AS crtDt, D.SENT_DT AS sentDt, D.COMPLETED_DT AS completedDt, " +
                "D.EXPIRES_DT AS expiresDt, T.TEMPLATE_NM AS templateNm, " +
                "(SELECT COUNT(*) FROM CONTRACT_SIGNER S WHERE S.CONTRACT_ID = D.CONTRACT_ID AND S.STATUS_CD = 'signed') AS signedCount, " +
                "(SELECT COUNT(*) FROM CONTRACT_SIGNER S WHERE S.CONTRACT_ID = D.CONTRACT_ID AND S.STATUS_CD IN ('pending', 'opened')) AS pendingCount " +
            "FROM CONTRACT_DOCUMENT D LEFT JOIN CONTRACT_TEMPLATE T ON T.TEMPLATE_ID = D.TEMPLATE_ID WHERE " + whereClause +
            " ORDER BY COALESCE(D.LAST_CHG_DT, D.CRT_DT) DESC LIMIT 6",
            params
        ).stream().map(this::normalizeContractSummary).toList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("counts", counts);
        response.put("recentContracts", recentContracts);
        response.put("templates", readTemplates(userId, admin).stream().limit(6).toList());
        response.put("templateRequests", readTemplateRequests(userId, admin).stream().limit(6).toList());
        response.put("companySettings", readCompanySettingsInternal());
        return response;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> readTemplates(String userId, boolean admin) {
        return namedJdbc.queryForList(
            "SELECT T.TEMPLATE_ID AS templateId, T.TEMPLATE_CODE AS templateCode, T.TEMPLATE_NM AS templateNm, T.TEMPLATE_DESC AS templateDesc, " +
                "T.TEMPLATE_CATEGORY_CD AS templateCategoryCd, T.ACTIVE_YN AS activeYn, T.REQUEST_ID AS requestId, T.PUBLISHED_VERSION_ID AS publishedVersionId, " +
                "T.CRT_USER_ID AS crtUserId, T.CRT_DT AS crtDt, T.LAST_CHG_DT AS lastChgDt, PV.VERSION_LABEL AS publishedVersionLabel, PV.VERSION_NO AS publishedVersionNo, " +
                "(SELECT MAX(V.VERSION_NO) FROM CONTRACT_TEMPLATE_VERSION V WHERE V.TEMPLATE_ID = T.TEMPLATE_ID) AS latestVersionNo, " +
                "(SELECT COUNT(*) FROM CONTRACT_DOCUMENT D WHERE D.TEMPLATE_ID = T.TEMPLATE_ID) AS usageCount " +
            "FROM CONTRACT_TEMPLATE T LEFT JOIN CONTRACT_TEMPLATE_VERSION PV ON PV.TEMPLATE_VERSION_ID = T.PUBLISHED_VERSION_ID " +
            "WHERE (:admin = TRUE OR COALESCE(T.ACTIVE_YN, 'Y') = 'Y') ORDER BY COALESCE(T.LAST_CHG_DT, T.CRT_DT) DESC, T.TEMPLATE_ID DESC",
            Map.of("admin", admin)
        ).stream().map(this::normalizeTemplateSummary).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> readTemplate(Long templateId, String userId, boolean admin) {
        Map<String, Object> template = findTemplate(templateId);
        if (!admin && !"Y".equalsIgnoreCase(stringValue(template.get("activeYn"), "Y"))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Template not found.");
        }

        List<Map<String, Object>> versions = namedJdbc.queryForList(
            "SELECT TEMPLATE_VERSION_ID AS templateVersionId, TEMPLATE_ID AS templateId, VERSION_NO AS versionNo, VERSION_LABEL AS versionLabel, " +
                "VERSION_STATUS_CD AS versionStatusCd, VERSION_NOTE AS versionNote, SOURCE_FILE_ID AS sourceFileId, BACKGROUND_FILE_ID AS backgroundFileId, " +
                "SCHEMA_JSON AS schemaJson, LAYOUT_JSON AS layoutJson, CRT_USER_ID AS crtUserId, CRT_DT AS crtDt, PUBLISHED_DT AS publishedDt, LAST_CHG_DT AS lastChgDt " +
            "FROM CONTRACT_TEMPLATE_VERSION WHERE TEMPLATE_ID = :templateId ORDER BY VERSION_NO DESC, TEMPLATE_VERSION_ID DESC",
            Map.of("templateId", templateId)
        ).stream().map(this::normalizeTemplateVersion).toList();

        Map<String, Object> response = new LinkedHashMap<>(template);
        response.put("versions", versions);
        response.put("currentVersion", resolveCurrentVersion(response, versions, admin));
        response.put("request", longValue(response.get("requestId")) == null ? null : findTemplateRequest(longValue(response.get("requestId"))));
        return response;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> readTemplateRequests(String userId, boolean admin) {
        return namedJdbc.queryForList(
            "SELECT R.REQUEST_ID AS requestId, R.REQUEST_REF AS requestRef, R.REQUEST_TITLE AS requestTitle, R.REQUEST_STATUS_CD AS requestStatusCd, " +
                "R.REQUEST_NOTE AS requestNote, R.REQUESTER_USER_ID AS requesterUserId, R.REVIEW_USER_ID AS reviewUserId, R.REVIEW_NOTE AS reviewNote, " +
                "R.REVIEW_DT AS reviewDt, R.CRT_DT AS crtDt, R.LAST_CHG_DT AS lastChgDt, R.SOURCE_FILE_ID AS sourceFileId, R.MARKED_FILE_ID AS markedFileId, " +
                "R.SEAL_FILE_ID AS sealFileId, R.CONVERTED_TEMPLATE_ID AS convertedTemplateId, U.USER_NM AS requesterNm, RU.USER_NM AS reviewUserNm " +
            "FROM CONTRACT_TEMPLATE_REQUEST R LEFT JOIN USERS U ON U.USER_ID = R.REQUESTER_USER_ID LEFT JOIN USERS RU ON RU.USER_ID = R.REVIEW_USER_ID " +
            "WHERE (:admin = TRUE OR R.REQUESTER_USER_ID = :userId) ORDER BY COALESCE(R.LAST_CHG_DT, R.CRT_DT) DESC, R.REQUEST_ID DESC",
            Map.of("admin", admin, "userId", userId)
        ).stream().map(this::normalizeTemplateRequest).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> readCompanySettings(String userId, boolean admin) {
        requireAdmin(admin);
        return readCompanySettingsInternal();
    }

    private Map<String, Object> readCompanySettingsInternal() {
        Long settingId = ensureCompanySettingsRow("system");
        List<Map<String, Object>> rows = namedJdbc.queryForList(
            "SELECT SETTING_ID AS settingId, COMPANY_NM AS companyNm, SENDER_NM AS senderNm, SENDER_EMAIL AS senderEmail, SENDER_TELNO AS senderTelno, " +
                "PROVIDER_NM AS providerNm, CHECKLIST_JSON AS checklistJson, NOTE_TEXT AS noteText, GUIDE_ACK_YN AS guideAckYn, ADMIN_READY_YN AS adminReadyYn, " +
                "SEAL_FILE_ID AS sealFileId, CRT_DT AS crtDt, LAST_CHG_DT AS lastChgDt, LAST_CHG_USER_ID AS lastChgUserId " +
            "FROM CONTRACT_COMPANY_SETTING WHERE SETTING_ID = :settingId",
            Map.of("settingId", settingId)
        );
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Company settings are unavailable.");
        }
        return normalizeCompanySettings(rows.get(0));
    }

    private Long ensureCompanySettingsRow(String userId) {
        List<Long> ids = namedJdbc.queryForList("SELECT SETTING_ID FROM CONTRACT_COMPANY_SETTING ORDER BY SETTING_ID ASC LIMIT 1", Map.of(), Long.class);
        if (!ids.isEmpty()) {
            return ids.get(0);
        }
        return namedJdbc.queryForObject(
            "INSERT INTO CONTRACT_COMPANY_SETTING (COMPANY_NM, SENDER_NM, SENDER_EMAIL, SENDER_TELNO, PROVIDER_NM, CRT_USER_ID, CRT_DT, LAST_CHG_DT, GUIDE_ACK_YN, ADMIN_READY_YN) " +
                "VALUES ('\\uBAA8\\uB450\\uC758 \\uB7EC\\uB2DD', '\\uAD00\\uB9AC\\uC790', 'admin@modulearning.local', '010-0000-0000', 'internal', :userId, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'N', 'N') RETURNING SETTING_ID",
            Map.of("userId", userId),
            Long.class
        );
    }

    private Map<String, Object> findTemplate(Long templateId) {
        List<Map<String, Object>> rows = namedJdbc.queryForList(
            "SELECT T.TEMPLATE_ID AS templateId, T.TEMPLATE_CODE AS templateCode, T.TEMPLATE_NM AS templateNm, T.TEMPLATE_DESC AS templateDesc, " +
                "T.TEMPLATE_CATEGORY_CD AS templateCategoryCd, T.ACTIVE_YN AS activeYn, T.REQUEST_ID AS requestId, T.PUBLISHED_VERSION_ID AS publishedVersionId, " +
                "T.CRT_USER_ID AS crtUserId, T.CRT_DT AS crtDt, T.LAST_CHG_DT AS lastChgDt, PV.VERSION_LABEL AS publishedVersionLabel, PV.VERSION_NO AS publishedVersionNo, " +
                "(SELECT MAX(V.VERSION_NO) FROM CONTRACT_TEMPLATE_VERSION V WHERE V.TEMPLATE_ID = T.TEMPLATE_ID) AS latestVersionNo, " +
                "(SELECT COUNT(*) FROM CONTRACT_DOCUMENT D WHERE D.TEMPLATE_ID = T.TEMPLATE_ID) AS usageCount " +
            "FROM CONTRACT_TEMPLATE T LEFT JOIN CONTRACT_TEMPLATE_VERSION PV ON PV.TEMPLATE_VERSION_ID = T.PUBLISHED_VERSION_ID WHERE T.TEMPLATE_ID = :templateId",
            Map.of("templateId", templateId)
        );
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Template not found.");
        }
        return normalizeTemplateSummary(rows.get(0));
    }

    private Map<String, Object> findTemplateRequest(Long requestId) {
        List<Map<String, Object>> rows = namedJdbc.queryForList(
            "SELECT R.REQUEST_ID AS requestId, R.REQUEST_REF AS requestRef, R.REQUEST_TITLE AS requestTitle, R.REQUEST_STATUS_CD AS requestStatusCd, R.REQUEST_NOTE AS requestNote, " +
                "R.REQUESTER_USER_ID AS requesterUserId, R.REVIEW_USER_ID AS reviewUserId, R.REVIEW_NOTE AS reviewNote, R.REVIEW_DT AS reviewDt, R.CRT_DT AS crtDt, " +
                "R.LAST_CHG_DT AS lastChgDt, R.SOURCE_FILE_ID AS sourceFileId, R.MARKED_FILE_ID AS markedFileId, R.SEAL_FILE_ID AS sealFileId, " +
                "R.CONVERTED_TEMPLATE_ID AS convertedTemplateId, U.USER_NM AS requesterNm, RU.USER_NM AS reviewUserNm " +
            "FROM CONTRACT_TEMPLATE_REQUEST R LEFT JOIN USERS U ON U.USER_ID = R.REQUESTER_USER_ID LEFT JOIN USERS RU ON RU.USER_ID = R.REVIEW_USER_ID " +
            "WHERE R.REQUEST_ID = :requestId",
            Map.of("requestId", requestId)
        );
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Template request not found.");
        }
        return normalizeTemplateRequest(rows.get(0));
    }

    @Transactional
    public Map<String, Object> createTemplate(
        Map<String, Object> payload,
        MultipartFile sourceFile,
        MultipartFile backgroundFile,
        String userId,
        boolean admin
    ) {
        requireAdmin(admin);
        String templateName = requiredText(payload.get("templateNm"), "Template name is required.");
        String templateCode = ensureUniqueTemplateCode(normalizeTemplateCode(payload.get("templateCode"), templateName), null);
        Long requestId = longValue(payload.get("requestId"));

        Long templateId = namedJdbc.queryForObject(
            "INSERT INTO CONTRACT_TEMPLATE (TEMPLATE_CODE, TEMPLATE_NM, TEMPLATE_DESC, TEMPLATE_CATEGORY_CD, ACTIVE_YN, REQUEST_ID, CRT_USER_ID, CRT_DT, LAST_CHG_DT) " +
                "VALUES (:templateCode, :templateNm, :templateDesc, :templateCategoryCd, :activeYn, :requestId, :userId, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) RETURNING TEMPLATE_ID",
            new MapSqlParameterSource()
                .addValue("templateCode", templateCode)
                .addValue("templateNm", templateName)
                .addValue("templateDesc", blankToNull(payload.get("templateDesc")))
                .addValue("templateCategoryCd", stringValue(payload.get("templateCategoryCd"), "general"))
                .addValue("activeYn", truthy(payload.get("activeYn"), true) ? "Y" : "N")
                .addValue("requestId", requestId)
                .addValue("userId", userId),
            Long.class
        );

        String sourceFileId = resolveUploadedFileId(sourceFile, stringValue(payload.get("sourceFileId")), FileFolderType.CONTRACT.toString() + "/template/source");
        String backgroundFileId = resolveUploadedFileId(backgroundFile, stringValue(payload.get("backgroundFileId")), FileFolderType.CONTRACT.toString() + "/template/background");
        Long versionId = createOrUpdateVersion(templateId, null, payload, sourceFileId, backgroundFileId, userId, false);

        if (requestId != null) {
            namedJdbc.update(
                "UPDATE CONTRACT_TEMPLATE_REQUEST SET CONVERTED_TEMPLATE_ID = :templateId, REQUEST_STATUS_CD = 'reviewing', LAST_CHG_DT = CURRENT_TIMESTAMP WHERE REQUEST_ID = :requestId",
                new MapSqlParameterSource().addValue("templateId", templateId).addValue("requestId", requestId)
            );
        }
        if ("published".equalsIgnoreCase(stringValue(payload.get("versionStatusCd")))) {
            publishTemplate(templateId, versionId, userId, admin);
        }
        return readTemplate(templateId, userId, admin);
    }

    @Transactional
    public Map<String, Object> updateTemplate(
        Long templateId,
        Map<String, Object> payload,
        MultipartFile sourceFile,
        MultipartFile backgroundFile,
        String userId,
        boolean admin
    ) {
        requireAdmin(admin);
        Map<String, Object> template = findTemplate(templateId);
        String templateName = requiredText(firstNonBlank(payload.get("templateNm"), template.get("templateNm")), "Template name is required.");
        String templateCode = ensureUniqueTemplateCode(normalizeTemplateCode(firstNonBlank(payload.get("templateCode"), template.get("templateCode")), templateName), templateId);

        namedJdbc.update(
            "UPDATE CONTRACT_TEMPLATE SET TEMPLATE_CODE = :templateCode, TEMPLATE_NM = :templateNm, TEMPLATE_DESC = :templateDesc, TEMPLATE_CATEGORY_CD = :templateCategoryCd, " +
                "ACTIVE_YN = :activeYn, LAST_CHG_DT = CURRENT_TIMESTAMP WHERE TEMPLATE_ID = :templateId",
            new MapSqlParameterSource()
                .addValue("templateId", templateId)
                .addValue("templateCode", templateCode)
                .addValue("templateNm", templateName)
                .addValue("templateDesc", blankToNull(firstNonBlank(payload.get("templateDesc"), template.get("templateDesc"))))
                .addValue("templateCategoryCd", stringValue(firstNonBlank(payload.get("templateCategoryCd"), template.get("templateCategoryCd")), "general"))
                .addValue("activeYn", truthy(firstNonBlank(payload.get("activeYn"), template.get("activeYn")), true) ? "Y" : "N")
        );

        String sourceFileId = resolveUploadedFileId(sourceFile, stringValue(payload.get("sourceFileId")), FileFolderType.CONTRACT.toString() + "/template/source");
        String backgroundFileId = resolveUploadedFileId(backgroundFile, stringValue(payload.get("backgroundFileId")), FileFolderType.CONTRACT.toString() + "/template/background");
        Long versionId = createOrUpdateVersion(templateId, longValue(payload.get("templateVersionId")), payload, sourceFileId, backgroundFileId, userId, true);
        if (truthy(payload.get("publishNow"), false)) {
            publishTemplate(templateId, versionId, userId, admin);
        }
        return readTemplate(templateId, userId, admin);
    }

    @Transactional
    public Map<String, Object> publishTemplate(Long templateId, Long requestedVersionId, String userId, boolean admin) {
        requireAdmin(admin);
        findTemplate(templateId);
        Long versionId = requestedVersionId;
        if (versionId == null) {
            versionId = namedJdbc.queryForObject(
                "SELECT TEMPLATE_VERSION_ID FROM CONTRACT_TEMPLATE_VERSION WHERE TEMPLATE_ID = :templateId ORDER BY VERSION_NO DESC, TEMPLATE_VERSION_ID DESC LIMIT 1",
                Map.of("templateId", templateId),
                Long.class
            );
        }
        if (versionId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "There is no template version to publish.");
        }

        namedJdbc.update(
            "UPDATE CONTRACT_TEMPLATE_VERSION SET VERSION_STATUS_CD = CASE WHEN TEMPLATE_VERSION_ID = :versionId THEN 'published' " +
                "WHEN VERSION_STATUS_CD = 'published' THEN 'archived' ELSE VERSION_STATUS_CD END, " +
                "PUBLISHED_DT = CASE WHEN TEMPLATE_VERSION_ID = :versionId THEN CURRENT_TIMESTAMP ELSE PUBLISHED_DT END, " +
                "LAST_CHG_DT = CURRENT_TIMESTAMP WHERE TEMPLATE_ID = :templateId",
            new MapSqlParameterSource().addValue("templateId", templateId).addValue("versionId", versionId)
        );
        namedJdbc.update(
            "UPDATE CONTRACT_TEMPLATE SET PUBLISHED_VERSION_ID = :versionId, ACTIVE_YN = 'Y', LAST_CHG_DT = CURRENT_TIMESTAMP WHERE TEMPLATE_ID = :templateId",
            new MapSqlParameterSource().addValue("templateId", templateId).addValue("versionId", versionId)
        );
        return readTemplate(templateId, userId, admin);
    }

    @Transactional
    public Map<String, Object> createTemplateRequest(
        Map<String, Object> payload,
        MultipartFile sourceFile,
        MultipartFile markedFile,
        MultipartFile sealFile,
        String userId
    ) {
        if ((sourceFile == null || sourceFile.isEmpty()) && StringUtils.isBlank(stringValue(payload.get("sourceFileId")))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Source file is required.");
        }
        if ((markedFile == null || markedFile.isEmpty()) && StringUtils.isBlank(stringValue(payload.get("markedFileId")))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Marked file is required.");
        }
        if ((sealFile == null || sealFile.isEmpty()) && StringUtils.isBlank(stringValue(payload.get("sealFileId")))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seal image is required.");
        }

        Long requestId = namedJdbc.queryForObject(
            "INSERT INTO CONTRACT_TEMPLATE_REQUEST (REQUEST_REF, REQUEST_TITLE, REQUEST_STATUS_CD, REQUEST_NOTE, REQUESTER_USER_ID, SOURCE_FILE_ID, MARKED_FILE_ID, SEAL_FILE_ID, CRT_DT, LAST_CHG_DT) " +
                "VALUES (:requestRef, :requestTitle, 'requested', :requestNote, :requesterUserId, :sourceFileId, :markedFileId, :sealFileId, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) RETURNING REQUEST_ID",
            new MapSqlParameterSource()
                .addValue("requestRef", generateExternalRef("CTRREQ"))
                .addValue("requestTitle", requiredText(payload.get("requestTitle"), "Request title is required."))
                .addValue("requestNote", blankToNull(payload.get("requestNote")))
                .addValue("requesterUserId", userId)
                .addValue("sourceFileId", resolveUploadedFileId(sourceFile, stringValue(payload.get("sourceFileId")), FileFolderType.CONTRACT.toString() + "/request/source"))
                .addValue("markedFileId", resolveUploadedFileId(markedFile, stringValue(payload.get("markedFileId")), FileFolderType.CONTRACT.toString() + "/request/marked"))
                .addValue("sealFileId", resolveUploadedFileId(sealFile, stringValue(payload.get("sealFileId")), FileFolderType.CONTRACT.toString() + "/request/seal")),
            Long.class
        );
        notifyAdmins("CONTRACT_REQUEST", userId, String.valueOf(requestId));
        return findTemplateRequest(requestId);
    }

    @Transactional
    public Map<String, Object> approveTemplateRequest(Long requestId, Map<String, Object> payload, String userId, boolean admin) {
        requireAdmin(admin);
        Map<String, Object> request = findTemplateRequest(requestId);
        Long templateId = longValue(request.get("convertedTemplateId"));
        if (templateId == null) {
            Map<String, Object> draftTemplate = new LinkedHashMap<>();
            draftTemplate.put("templateNm", request.get("requestTitle"));
            draftTemplate.put("templateDesc", firstNonBlank(payload == null ? null : payload.get("templateDesc"), request.get("requestNote")));
            draftTemplate.put("templateCategoryCd", stringValue(payload == null ? null : payload.get("templateCategoryCd"), "custom"));
            draftTemplate.put("requestId", requestId);
            draftTemplate.put("schema", defaultSchemaForRequest(stringValue(request.get("requestTitle"), "Contract")));
            draftTemplate.put("layout", defaultLayout());
            templateId = longValue(createTemplate(draftTemplate, null, null, userId, admin).get("templateId"));
        }

        namedJdbc.update(
            "UPDATE CONTRACT_TEMPLATE_REQUEST SET REQUEST_STATUS_CD = 'approved', REVIEW_USER_ID = :reviewUserId, REVIEW_NOTE = :reviewNote, REVIEW_DT = CURRENT_TIMESTAMP, " +
                "LAST_CHG_DT = CURRENT_TIMESTAMP, CONVERTED_TEMPLATE_ID = :templateId WHERE REQUEST_ID = :requestId",
            new MapSqlParameterSource()
                .addValue("requestId", requestId)
                .addValue("reviewUserId", userId)
                .addValue("reviewNote", blankToNull(payload == null ? null : payload.get("reviewNote")))
                .addValue("templateId", templateId)
        );
        if (StringUtils.isNotBlank(stringValue(request.get("requesterUserId"))) && !Objects.equals(request.get("requesterUserId"), userId)) {
            sendNotification(stringValue(request.get("requesterUserId")), userId, "CONTRACT_TEMPLATE_APPROVED", String.valueOf(templateId));
        }
        return findTemplateRequest(requestId);
    }

    @Transactional
    public Map<String, Object> rejectTemplateRequest(Long requestId, Map<String, Object> payload, String userId, boolean admin) {
        requireAdmin(admin);
        findTemplateRequest(requestId);
        namedJdbc.update(
            "UPDATE CONTRACT_TEMPLATE_REQUEST SET REQUEST_STATUS_CD = 'rejected', REVIEW_USER_ID = :reviewUserId, REVIEW_NOTE = :reviewNote, REVIEW_DT = CURRENT_TIMESTAMP, LAST_CHG_DT = CURRENT_TIMESTAMP WHERE REQUEST_ID = :requestId",
            new MapSqlParameterSource()
                .addValue("requestId", requestId)
                .addValue("reviewUserId", userId)
                .addValue("reviewNote", blankToNull(payload == null ? null : payload.get("reviewNote")))
        );
        return findTemplateRequest(requestId);
    }

    @Transactional
    public Map<String, Object> updateCompanySettings(Map<String, Object> payload, MultipartFile sealFile, String userId, boolean admin) {
        requireAdmin(admin);
        Long settingId = ensureCompanySettingsRow(userId);
        namedJdbc.update(
            "UPDATE CONTRACT_COMPANY_SETTING SET COMPANY_NM = :companyNm, SENDER_NM = :senderNm, SENDER_EMAIL = :senderEmail, SENDER_TELNO = :senderTelno, PROVIDER_NM = :providerNm, " +
                "CHECKLIST_JSON = :checklistJson, NOTE_TEXT = :noteText, GUIDE_ACK_YN = :guideAckYn, ADMIN_READY_YN = :adminReadyYn, SEAL_FILE_ID = :sealFileId, " +
                "LAST_CHG_DT = CURRENT_TIMESTAMP, LAST_CHG_USER_ID = :userId WHERE SETTING_ID = :settingId",
            new MapSqlParameterSource()
                .addValue("settingId", settingId)
                .addValue("companyNm", blankToNull(payload.get("companyNm")))
                .addValue("senderNm", blankToNull(payload.get("senderNm")))
                .addValue("senderEmail", blankToNull(payload.get("senderEmail")))
                .addValue("senderTelno", blankToNull(payload.get("senderTelno")))
                .addValue("providerNm", blankToNull(payload.get("providerNm")))
                .addValue("checklistJson", toJson(payload.get("checklist")))
                .addValue("noteText", blankToNull(payload.get("noteText")))
                .addValue("guideAckYn", truthy(payload.get("guideAckYn"), false) ? "Y" : "N")
                .addValue("adminReadyYn", truthy(payload.get("adminReadyYn"), false) ? "Y" : "N")
                .addValue("sealFileId", resolveUploadedFileId(sealFile, stringValue(payload.get("sealFileId")), FileFolderType.CONTRACT.toString() + "/company"))
                .addValue("userId", userId)
        );
        return readCompanySettingsInternal();
    }

    @Transactional
    public Map<String, Object> readContracts(String userId, boolean admin, Map<String, String> params) {
        expireOutdatedContracts();
        MapSqlParameterSource queryParams = accessParams(userId, admin);
        StringBuilder where = new StringBuilder(contractAccessWhere());

        String section = stringValue(params.get("section"), "all");
        if ("draft".equalsIgnoreCase(section)) {
            where.append(" AND D.STATUS_CD IN ('draft', 'ready')");
        } else if ("active".equalsIgnoreCase(section) || "in_progress".equalsIgnoreCase(section)) {
            where.append(" AND D.STATUS_CD IN ('sent', 'in_progress')");
        } else if ("completed".equalsIgnoreCase(section)) {
            where.append(" AND D.STATUS_CD = 'completed'");
        } else if ("archive".equalsIgnoreCase(section) || "expired".equalsIgnoreCase(section)) {
            where.append(" AND D.STATUS_CD IN ('expired', 'cancelled')");
        }

        if (StringUtils.isNotBlank(params.get("status"))) {
            where.append(" AND D.STATUS_CD = :statusCd");
            queryParams.addValue("statusCd", params.get("status"));
        }
        if (StringUtils.isNotBlank(params.get("templateId"))) {
            where.append(" AND D.TEMPLATE_ID = :templateId");
            queryParams.addValue("templateId", Long.valueOf(params.get("templateId")));
        }
        if (StringUtils.isNotBlank(params.get("q"))) {
            where.append(" AND (LOWER(D.CONTRACT_TITLE) LIKE :q OR LOWER(COALESCE(D.CONTRACT_MESSAGE, '')) LIKE :q OR LOWER(COALESCE(T.TEMPLATE_NM, '')) LIKE :q)");
            queryParams.addValue("q", "%" + params.get("q").trim().toLowerCase(Locale.ROOT) + "%");
        }

        int page = intValue(params.get("page"), 1);
        int pageSize = Math.min(30, Math.max(1, intValue(params.get("pageSize"), 12)));
        queryParams.addValue("limit", pageSize);
        queryParams.addValue("offset", Math.max(page - 1, 0) * pageSize);

        int totalRecords = queryCount("SELECT COUNT(*) FROM CONTRACT_DOCUMENT D LEFT JOIN CONTRACT_TEMPLATE T ON T.TEMPLATE_ID = D.TEMPLATE_ID WHERE " + where, queryParams);
        List<Map<String, Object>> items = namedJdbc.queryForList(
            "SELECT D.CONTRACT_ID AS contractId, D.CONTRACT_REF AS contractRef, D.CONTRACT_TITLE AS contractTitle, D.CONTRACT_MESSAGE AS contractMessage, D.STATUS_CD AS statusCd, " +
                "D.SEND_TYPE_CD AS sendTypeCd, D.SIGNING_FLOW_CD AS signingFlowCd, D.CREATOR_USER_ID AS creatorUserId, U.USER_NM AS creatorNm, D.CRT_DT AS crtDt, D.SENT_DT AS sentDt, " +
                "D.COMPLETED_DT AS completedDt, D.EXPIRES_DT AS expiresDt, D.CANCELLED_DT AS cancelledDt, D.CURRENT_SIGN_ORDER AS currentSignOrder, D.TOTAL_SIGNER_COUNT AS totalSignerCount, " +
                "D.FINAL_FILE_ID AS finalFileId, T.TEMPLATE_ID AS templateId, T.TEMPLATE_NM AS templateNm, T.TEMPLATE_CATEGORY_CD AS templateCategoryCd, " +
                "(SELECT COUNT(*) FROM CONTRACT_SIGNER S WHERE S.CONTRACT_ID = D.CONTRACT_ID AND S.STATUS_CD = 'signed') AS signedCount, " +
                "(SELECT COUNT(*) FROM CONTRACT_SIGNER S WHERE S.CONTRACT_ID = D.CONTRACT_ID AND S.STATUS_CD IN ('pending', 'opened')) AS pendingCount " +
            "FROM CONTRACT_DOCUMENT D LEFT JOIN CONTRACT_TEMPLATE T ON T.TEMPLATE_ID = D.TEMPLATE_ID LEFT JOIN USERS U ON U.USER_ID = D.CREATOR_USER_ID WHERE " + where +
            " ORDER BY COALESCE(D.LAST_CHG_DT, D.CRT_DT) DESC, D.CONTRACT_ID DESC LIMIT :limit OFFSET :offset",
            queryParams
        ).stream().map(this::normalizeContractSummary).toList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("items", items);
        response.put("page", page);
        response.put("pageSize", pageSize);
        response.put("totalRecords", totalRecords);
        response.put("totalPages", totalRecords == 0 ? 1 : (int) Math.ceil((double) totalRecords / pageSize));
        response.put("counts", readDashboard(userId, admin).get("counts"));
        return response;
    }

    @Transactional
    public Map<String, Object> readContract(Long contractId, String userId, boolean admin) {
        expireOutdatedContracts();
        Map<String, Object> contract = findContract(contractId);
        ensureContractReadable(contract, userId, admin);
        return hydrateContract(contract, userId, admin);
    }

    @Transactional
    public Map<String, Object> createContract(Map<String, Object> payload, String userId, boolean admin) {
        Long templateId = longValue(payload.get("templateId"));
        if (templateId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Template is required.");
        }

        Map<String, Object> template = readTemplate(templateId, userId, admin);
        Map<String, Object> version = resolveVersionForContract(template, longValue(payload.get("templateVersionId")), admin);
        Map<String, Object> settings = readCompanySettingsInternal();
        List<Map<String, Object>> signers = normalizeSignerPayload(payload.get("signers"));
        if (signers.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one signer is required.");
        }

        Long contractId = nextSequenceValue("CONTRACT_ID_SEQ");
        LocalDateTime expiresDt = localDateTime(firstNonBlank(payload.get("expiresDt"), LocalDateTime.now().plusDays(7)));
        namedJdbc.update(
            "INSERT INTO CONTRACT_DOCUMENT (CONTRACT_ID, CONTRACT_REF, TEMPLATE_ID, TEMPLATE_VERSION_ID, BATCH_ID, CONTRACT_TITLE, CONTRACT_MESSAGE, SEND_TYPE_CD, SIGNING_FLOW_CD, " +
                "STATUS_CD, CREATOR_USER_ID, SENDER_NM, SENDER_EMAIL, SENDER_TELNO, COMPANY_SNAPSHOT_JSON, TEMPLATE_SCHEMA_JSON, TEMPLATE_LAYOUT_JSON, EXPIRES_DT, TOTAL_SIGNER_COUNT, CURRENT_SIGN_ORDER, CRT_DT, LAST_CHG_DT) " +
                "VALUES (:contractId, :contractRef, :templateId, :templateVersionId, :batchId, :contractTitle, :contractMessage, :sendTypeCd, :signingFlowCd, :statusCd, :creatorUserId, " +
                ":senderNm, :senderEmail, :senderTelno, :companySnapshotJson, :templateSchemaJson, :templateLayoutJson, :expiresDt, :totalSignerCount, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            new MapSqlParameterSource()
                .addValue("contractId", contractId)
                .addValue("contractRef", generateContractRef(contractId))
                .addValue("templateId", templateId)
                .addValue("templateVersionId", longValue(version.get("templateVersionId")))
                .addValue("batchId", longValue(payload.get("batchId")))
                .addValue("contractTitle", requiredText(payload.get("contractTitle"), "Contract title is required."))
                .addValue("contractMessage", blankToNull(payload.get("contractMessage")))
                .addValue("sendTypeCd", normalizeEnum(payload.get("sendTypeCd"), Set.of("remote", "link", "bulk"), "remote"))
                .addValue("signingFlowCd", normalizeEnum(payload.get("signingFlowCd"), Set.of("parallel", "sequential"), "parallel"))
                .addValue("statusCd", normalizeEnum(payload.get("statusCd"), Set.of("draft", "ready"), "draft"))
                .addValue("creatorUserId", userId)
                .addValue("senderNm", blankToNull(firstNonBlank(payload.get("senderNm"), settings.get("senderNm"))))
                .addValue("senderEmail", blankToNull(firstNonBlank(payload.get("senderEmail"), settings.get("senderEmail"))))
                .addValue("senderTelno", blankToNull(firstNonBlank(payload.get("senderTelno"), settings.get("senderTelno"))))
                .addValue("companySnapshotJson", toJson(buildCompanySnapshot(settings)))
                .addValue("templateSchemaJson", toJson(version.get("schema")))
                .addValue("templateLayoutJson", toJson(version.get("layout")))
                .addValue("expiresDt", expiresDt)
                .addValue("totalSignerCount", signers.size())
        );

        persistContractSigners(contractId, signers);
        persistFieldValues(contractId, null, payload.get("fieldValues"), true);
        addAudit(contractId, null, "created", "Contract draft created", userId, "internal", userId, toJson(payload));
        return readContract(contractId, userId, admin);
    }

    @Transactional
    public Map<String, Object> sendContract(Long contractId, String userId, boolean admin) {
        expireOutdatedContracts();
        Map<String, Object> contract = findContract(contractId);
        ensureContractManageable(contract, userId, admin);
        if ("completed".equals(contract.get("statusCd")) || "cancelled".equals(contract.get("statusCd"))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This contract cannot be sent.");
        }
        List<Map<String, Object>> links = issueLinks(contractId, pendingOrOpenedSigners(contractId), userId, "sent", "Link issued from workspace");
        namedJdbc.update(
            "UPDATE CONTRACT_DOCUMENT SET STATUS_CD = CASE WHEN STATUS_CD IN ('draft', 'ready') THEN 'sent' ELSE STATUS_CD END, SENT_DT = COALESCE(SENT_DT, CURRENT_TIMESTAMP), LAST_CHG_DT = CURRENT_TIMESTAMP WHERE CONTRACT_ID = :contractId",
            Map.of("contractId", contractId)
        );
        refreshCurrentSignOrder(contractId);
        addAudit(contractId, null, "sent", "Contract links issued", userId, "internal", userId, toJson(Map.of("linkCount", links.size())));
        notifyInternalSigners(contractId, userId, "CONTRACT_SENT");
        return Map.of("contract", readContract(contractId, userId, admin), "links", links);
    }

    @Transactional
    public Map<String, Object> cancelContract(Long contractId, Map<String, Object> payload, String userId, boolean admin) {
        expireOutdatedContracts();
        Map<String, Object> contract = findContract(contractId);
        ensureContractManageable(contract, userId, admin);
        if ("completed".equals(contract.get("statusCd"))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Completed contracts cannot be cancelled.");
        }
        namedJdbc.update(
            "UPDATE CONTRACT_DOCUMENT SET STATUS_CD = 'cancelled', CANCELLED_DT = CURRENT_TIMESTAMP, CANCELLED_REASON = :reason, LAST_CHG_DT = CURRENT_TIMESTAMP WHERE CONTRACT_ID = :contractId",
            new MapSqlParameterSource().addValue("contractId", contractId).addValue("reason", blankToNull(payload == null ? null : payload.get("reason")))
        );
        namedJdbc.update(
            "UPDATE CONTRACT_INVITATION SET ACTIVE_YN = 'N', LAST_CHG_DT = CURRENT_TIMESTAMP WHERE CONTRACT_ID = :contractId AND COALESCE(ACTIVE_YN, 'Y') = 'Y'",
            Map.of("contractId", contractId)
        );
        addAudit(contractId, null, "cancelled", "Contract cancelled", userId, "internal", userId, payload == null ? null : toJson(payload));
        return readContract(contractId, userId, admin);
    }

    @Transactional
    public Map<String, Object> remindContract(Long contractId, String userId, boolean admin) {
        expireOutdatedContracts();
        Map<String, Object> contract = findContract(contractId);
        ensureContractManageable(contract, userId, admin);
        List<Map<String, Object>> signers = pendingOrOpenedSigners(contractId);
        if (signers.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "There are no pending signers.");
        }
        List<Map<String, Object>> links = issueLinks(contractId, signers, userId, "resent", "Reminder link issued");
        addAudit(contractId, null, "reminded", "Reminder links issued", userId, "internal", userId, toJson(Map.of("linkCount", links.size())));
        notifyInternalSigners(contractId, userId, "CONTRACT_REMINDER");
        return Map.of("contract", readContract(contractId, userId, admin), "links", links);
    }

    @Transactional
    public List<Map<String, Object>> readContractLinks(Long contractId, String userId, boolean admin) {
        Map<String, Object> contract = findContract(contractId);
        ensureContractManageable(contract, userId, admin);
        return issueLinks(contractId, pendingOrOpenedSigners(contractId), userId, "resent", "Workspace link refresh");
    }

    @Transactional
    public Map<String, Object> createBatch(Map<String, Object> payload, String userId, boolean admin) {
        Long templateId = longValue(payload.get("templateId"));
        if (templateId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Template is required.");
        }
        List<Map<String, Object>> rows = normalizeBatchRows(payload.get("rows"));
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Batch recipients are required.");
        }

        Long batchId = namedJdbc.queryForObject(
            "INSERT INTO CONTRACT_BATCH (BATCH_REF, TEMPLATE_ID, TEMPLATE_VERSION_ID, BATCH_TITLE, CREATOR_USER_ID, TOTAL_COUNT, SUCCESS_COUNT, FAILED_COUNT, RESULT_JSON, CRT_DT, LAST_CHG_DT) " +
                "VALUES (:batchRef, :templateId, :templateVersionId, :batchTitle, :creatorUserId, :totalCount, 0, 0, :resultJson, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) RETURNING BATCH_ID",
            new MapSqlParameterSource()
                .addValue("batchRef", generateExternalRef("CTRBATCH"))
                .addValue("templateId", templateId)
                .addValue("templateVersionId", longValue(payload.get("templateVersionId")))
                .addValue("batchTitle", requiredText(payload.get("batchTitle"), "Batch title is required."))
                .addValue("creatorUserId", userId)
                .addValue("totalCount", rows.size())
                .addValue("resultJson", toJson(Map.of("createdContracts", List.of()))),
            Long.class
        );

        List<String> createdRefs = new ArrayList<>();
        int successCount = 0;
        for (Map<String, Object> row : rows) {
            try {
                Map<String, Object> contractPayload = new LinkedHashMap<>(payload);
                contractPayload.put("batchId", batchId);
                contractPayload.put("sendTypeCd", "bulk");
                contractPayload.put("contractTitle", firstNonBlank(row.get("contractTitle"), payload.get("contractTitle"), payload.get("batchTitle")));
                contractPayload.put("contractMessage", firstNonBlank(row.get("contractMessage"), payload.get("contractMessage")));
                contractPayload.put("signers", List.of(normalizeSingleSignerRow(row)));
                contractPayload.put("fieldValues", row.get("fieldValues"));
                Map<String, Object> created = createContract(contractPayload, userId, admin);
                sendContract(longValue(created.get("contractId")), userId, admin);
                createdRefs.add(stringValue(created.get("contractRef")));
                successCount++;
            } catch (RuntimeException ex) {
                log.warn("Failed to create batch contract row {}", row, ex);
            }
        }

        namedJdbc.update(
            "UPDATE CONTRACT_BATCH SET SUCCESS_COUNT = :successCount, FAILED_COUNT = :failedCount, RESULT_JSON = :resultJson, LAST_CHG_DT = CURRENT_TIMESTAMP WHERE BATCH_ID = :batchId",
            new MapSqlParameterSource()
                .addValue("batchId", batchId)
                .addValue("successCount", successCount)
                .addValue("failedCount", rows.size() - successCount)
                .addValue("resultJson", toJson(Map.of("createdContracts", createdRefs)))
        );
        return readBatch(batchId, userId, admin);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> readBatch(Long batchId, String userId, boolean admin) {
        List<Map<String, Object>> rows = namedJdbc.queryForList(
            "SELECT BATCH_ID AS batchId, BATCH_REF AS batchRef, BATCH_TITLE AS batchTitle, CREATOR_USER_ID AS creatorUserId, CRT_DT AS crtDt, LAST_CHG_DT AS lastChgDt, " +
                "TEMPLATE_ID AS templateId, TEMPLATE_VERSION_ID AS templateVersionId, TOTAL_COUNT AS totalCount, SUCCESS_COUNT AS successCount, FAILED_COUNT AS failedCount, RESULT_JSON AS resultJson " +
            "FROM CONTRACT_BATCH WHERE BATCH_ID = :batchId",
            Map.of("batchId", batchId)
        );
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Batch not found.");
        }
        Map<String, Object> batch = normalizeBatch(rows.get(0));
        if (!admin && !Objects.equals(batch.get("creatorUserId"), userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have access to this batch.");
        }
        List<Map<String, Object>> contracts = namedJdbc.queryForList(
            "SELECT CONTRACT_ID AS contractId, CONTRACT_REF AS contractRef, CONTRACT_TITLE AS contractTitle, STATUS_CD AS statusCd, SENT_DT AS sentDt, COMPLETED_DT AS completedDt, EXPIRES_DT AS expiresDt " +
            "FROM CONTRACT_DOCUMENT WHERE BATCH_ID = :batchId ORDER BY CONTRACT_ID ASC",
            Map.of("batchId", batchId)
        ).stream().map(this::normalizeContractSummary).toList();
        batch.put("contracts", contracts);
        return batch;
    }

    @Transactional
    public Map<String, Object> readPublicContract(String token) {
        expireOutdatedContracts();
        InvitationContext context = resolveInvitation(token, false);
        markOpened(context);
        return buildPublicResponse(context);
    }

    @Transactional
    public Map<String, Object> claimPublicContract(String token, Map<String, Object> payload) {
        InvitationContext context = resolveInvitation(token, true);
        markOpened(context);
        namedJdbc.update(
            "UPDATE CONTRACT_SIGNER SET CLAIMED_NM = :claimedNm, CLAIMED_EMAIL = :claimedEmail, CLAIMED_TELNO = :claimedTelno, LAST_CHG_DT = CURRENT_TIMESTAMP WHERE SIGNER_ID = :signerId",
            new MapSqlParameterSource()
                .addValue("signerId", context.signerId)
                .addValue("claimedNm", blankToNull(firstNonBlank(payload.get("signerNm"), payload.get("claimedNm"), context.signer.get("claimedNm"), context.signer.get("signerNm"))))
                .addValue("claimedEmail", blankToNull(firstNonBlank(payload.get("signerEmail"), payload.get("claimedEmail"), context.signer.get("claimedEmail"), context.signer.get("signerEmail"))))
                .addValue("claimedTelno", blankToNull(firstNonBlank(payload.get("signerTelno"), payload.get("claimedTelno"), context.signer.get("claimedTelno"), context.signer.get("signerTelno"))))
        );
        return readPublicContract(token);
    }

    @Transactional
    public Map<String, Object> submitPublicContract(String token, Map<String, Object> payload) {
        InvitationContext context = resolveInvitation(token, true);
        markOpened(context);
        ensurePublicSubmitAllowed(context);

        namedJdbc.update(
            "UPDATE CONTRACT_SIGNER SET STATUS_CD = 'signed', OPENED_DT = COALESCE(OPENED_DT, CURRENT_TIMESTAMP), SIGNED_DT = CURRENT_TIMESTAMP, SIGNATURE_DATA = :signatureData, " +
                "INITIAL_DATA = :initialData, CLAIMED_NM = :claimedNm, CLAIMED_EMAIL = :claimedEmail, CLAIMED_TELNO = :claimedTelno, LAST_CHG_DT = CURRENT_TIMESTAMP WHERE SIGNER_ID = :signerId",
            new MapSqlParameterSource()
                .addValue("signerId", context.signerId)
                .addValue("signatureData", requiredText(firstNonBlank(payload.get("signatureData"), payload.get("signature"), payload.get("typedSignature")), "Signature is required."))
                .addValue("initialData", blankToNull(firstNonBlank(payload.get("initialData"), payload.get("initial"))))
                .addValue("claimedNm", blankToNull(firstNonBlank(payload.get("signerNm"), payload.get("claimedNm"), context.signer.get("claimedNm"), context.signer.get("signerNm"))))
                .addValue("claimedEmail", blankToNull(firstNonBlank(payload.get("signerEmail"), payload.get("claimedEmail"), context.signer.get("claimedEmail"), context.signer.get("signerEmail"))))
                .addValue("claimedTelno", blankToNull(firstNonBlank(payload.get("signerTelno"), payload.get("claimedTelno"), context.signer.get("claimedTelno"), context.signer.get("signerTelno"))))
        );
        namedJdbc.update(
            "UPDATE CONTRACT_INVITATION SET COMPLETED_DT = CURRENT_TIMESTAMP, DELIVERY_STATUS_CD = 'completed', LAST_CHG_DT = CURRENT_TIMESTAMP WHERE INVITATION_ID = :invitationId",
            Map.of("invitationId", context.invitationId)
        );
        persistFieldValues(context.contractId, context.signerId, payload.get("fieldValues"), false);
        addAudit(context.contractId, context.signerId, "signed", "Public signer completed signing", null, "external", context.signer.get("signerNm"), toJson(payload));

        if (pendingOrOpenedSigners(context.contractId).isEmpty()) {
            completeContract(context.contractId);
        } else {
            refreshCurrentSignOrder(context.contractId);
            namedJdbc.update("UPDATE CONTRACT_DOCUMENT SET STATUS_CD = 'in_progress', LAST_CHG_DT = CURRENT_TIMESTAMP WHERE CONTRACT_ID = :contractId", Map.of("contractId", context.contractId));
        }
        return readPublicContract(token);
    }

    @Transactional(readOnly = true)
    public FileDownloadPayload readPublicDownload(String token) {
        InvitationContext context = resolveInvitation(token, false);
        if (!"completed".equals(context.contract.get("statusCd"))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The contract is not completed yet.");
        }
        String fileId = stringValue(context.contract.get("finalFileId"));
        if (StringUtils.isBlank(fileId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Final PDF is not available.");
        }
        List<FileDetailVO> details = fileDetailService.readFileDetailList(fileId);
        if (details.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Final PDF file was not found.");
        }
        FileDetailVO detail = details.get(0);
        return new FileDownloadPayload(detail.getOrgnFileNm(), detail.getFileMimeType(), readFileBytes(detail));
    }

    private Long nextSequenceValue(String sequenceName) {
        return namedJdbc.queryForObject("SELECT nextval('" + sequenceName + "')", Map.of(), Long.class);
    }

    private String normalizeEnum(Object rawValue, Set<String> allowedValues, String defaultValue) {
        String value = stringValue(rawValue, defaultValue).toLowerCase(Locale.ROOT);
        return allowedValues.contains(value) ? value : defaultValue;
    }

    private Object firstNonBlank(Object... values) {
        for (Object value : values) {
            if (value == null) {
                continue;
            }
            if (value instanceof String text) {
                if (StringUtils.isNotBlank(text)) {
                    return text;
                }
                continue;
            }
            return value;
        }
        return null;
    }

    private String generateExternalRef(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase(Locale.ROOT);
    }

    private String generateContractRef(Long contractId) {
        return "CTRDOC" + String.format("%08d", contractId);
    }

    private String generateRawToken() {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
    }

    private String sha256(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte value : hashed) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable.", ex);
        }
    }

    private Map<String, Object> normalizeContractSummary(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("contractId", longValue(row.get("contractId")));
        result.put("contractRef", stringValue(row.get("contractRef")));
        result.put("contractTitle", stringValue(row.get("contractTitle")));
        result.put("contractMessage", stringValue(row.get("contractMessage")));
        result.put("statusCd", stringValue(row.get("statusCd")));
        result.put("sendTypeCd", stringValue(row.get("sendTypeCd")));
        result.put("signingFlowCd", stringValue(row.get("signingFlowCd")));
        result.put("creatorUserId", stringValue(row.get("creatorUserId")));
        result.put("creatorNm", stringValue(row.get("creatorNm")));
        result.put("templateId", longValue(row.get("templateId")));
        result.put("templateNm", stringValue(row.get("templateNm")));
        result.put("templateCategoryCd", stringValue(row.get("templateCategoryCd")));
        result.put("crtDt", localDateTime(row.get("crtDt")));
        result.put("sentDt", localDateTime(row.get("sentDt")));
        result.put("completedDt", localDateTime(row.get("completedDt")));
        result.put("cancelledDt", localDateTime(row.get("cancelledDt")));
        result.put("expiresDt", localDateTime(row.get("expiresDt")));
        result.put("signedCount", intValue(row.get("signedCount"), 0));
        result.put("pendingCount", intValue(row.get("pendingCount"), 0));
        result.put("currentSignOrder", intValue(row.get("currentSignOrder"), 1));
        result.put("totalSignerCount", intValue(row.get("totalSignerCount"), 0));
        result.put("finalFileId", stringValue(row.get("finalFileId")));
        return result;
    }

    private Map<String, Object> normalizeTemplateSummary(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("templateId", longValue(row.get("templateId")));
        result.put("templateCode", stringValue(row.get("templateCode")));
        result.put("templateNm", stringValue(row.get("templateNm")));
        result.put("templateDesc", stringValue(row.get("templateDesc")));
        result.put("templateCategoryCd", stringValue(row.get("templateCategoryCd"), "general"));
        result.put("activeYn", stringValue(row.get("activeYn"), "Y"));
        result.put("requestId", longValue(row.get("requestId")));
        result.put("publishedVersionId", longValue(row.get("publishedVersionId")));
        result.put("publishedVersionLabel", stringValue(row.get("publishedVersionLabel")));
        result.put("publishedVersionNo", intValue(row.get("publishedVersionNo"), 0));
        result.put("latestVersionNo", intValue(row.get("latestVersionNo"), 0));
        result.put("usageCount", intValue(row.get("usageCount"), 0));
        result.put("crtUserId", stringValue(row.get("crtUserId")));
        result.put("crtDt", localDateTime(row.get("crtDt")));
        result.put("lastChgDt", localDateTime(row.get("lastChgDt")));
        return result;
    }

    private Map<String, Object> normalizeTemplateVersion(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("templateVersionId", longValue(row.get("templateVersionId")));
        result.put("templateId", longValue(row.get("templateId")));
        result.put("versionNo", intValue(row.get("versionNo"), 0));
        result.put("versionLabel", stringValue(row.get("versionLabel")));
        result.put("versionStatusCd", stringValue(row.get("versionStatusCd"), "draft"));
        result.put("versionNote", stringValue(row.get("versionNote")));
        result.put("schema", parseJson(stringValue(row.get("schemaJson"))));
        result.put("layout", parseJson(stringValue(row.get("layoutJson"))));
        result.put("sourceFileId", stringValue(row.get("sourceFileId")));
        result.put("backgroundFileId", stringValue(row.get("backgroundFileId")));
        result.put("sourceFile", buildFileSummary(stringValue(row.get("sourceFileId"))));
        result.put("backgroundFile", buildFileSummary(stringValue(row.get("backgroundFileId"))));
        result.put("crtUserId", stringValue(row.get("crtUserId")));
        result.put("crtDt", localDateTime(row.get("crtDt")));
        result.put("publishedDt", localDateTime(row.get("publishedDt")));
        result.put("lastChgDt", localDateTime(row.get("lastChgDt")));
        return result;
    }

    private Map<String, Object> normalizeTemplateRequest(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("requestId", longValue(row.get("requestId")));
        result.put("requestRef", stringValue(row.get("requestRef")));
        result.put("requestTitle", stringValue(row.get("requestTitle")));
        result.put("requestStatusCd", stringValue(row.get("requestStatusCd"), "requested"));
        result.put("requestNote", stringValue(row.get("requestNote")));
        result.put("requesterUserId", stringValue(row.get("requesterUserId")));
        result.put("requesterNm", stringValue(row.get("requesterNm")));
        result.put("reviewUserId", stringValue(row.get("reviewUserId")));
        result.put("reviewUserNm", stringValue(row.get("reviewUserNm")));
        result.put("reviewNote", stringValue(row.get("reviewNote")));
        result.put("reviewDt", localDateTime(row.get("reviewDt")));
        result.put("crtDt", localDateTime(row.get("crtDt")));
        result.put("lastChgDt", localDateTime(row.get("lastChgDt")));
        result.put("sourceFileId", stringValue(row.get("sourceFileId")));
        result.put("markedFileId", stringValue(row.get("markedFileId")));
        result.put("sealFileId", stringValue(row.get("sealFileId")));
        result.put("sourceFile", buildFileSummary(stringValue(row.get("sourceFileId"))));
        result.put("markedFile", buildFileSummary(stringValue(row.get("markedFileId"))));
        result.put("sealFile", buildFileSummary(stringValue(row.get("sealFileId"))));
        result.put("convertedTemplateId", longValue(row.get("convertedTemplateId")));
        return result;
    }

    private Map<String, Object> normalizeCompanySettings(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("settingId", longValue(row.get("settingId")));
        result.put("companyNm", stringValue(row.get("companyNm")));
        result.put("senderNm", stringValue(row.get("senderNm")));
        result.put("senderEmail", stringValue(row.get("senderEmail")));
        result.put("senderTelno", stringValue(row.get("senderTelno")));
        result.put("providerNm", stringValue(row.get("providerNm")));
        result.put("checklist", parseJson(stringValue(row.get("checklistJson"))));
        result.put("noteText", stringValue(row.get("noteText")));
        result.put("guideAckYn", stringValue(row.get("guideAckYn"), "N"));
        result.put("adminReadyYn", stringValue(row.get("adminReadyYn"), "N"));
        result.put("sealFileId", stringValue(row.get("sealFileId")));
        result.put("sealFile", buildFileSummary(stringValue(row.get("sealFileId"))));
        result.put("crtDt", localDateTime(row.get("crtDt")));
        result.put("lastChgDt", localDateTime(row.get("lastChgDt")));
        result.put("lastChgUserId", stringValue(row.get("lastChgUserId")));
        return result;
    }

    private Map<String, Object> normalizeBatch(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("batchId", longValue(row.get("batchId")));
        result.put("batchRef", stringValue(row.get("batchRef")));
        result.put("batchTitle", stringValue(row.get("batchTitle")));
        result.put("creatorUserId", stringValue(row.get("creatorUserId")));
        result.put("crtDt", localDateTime(row.get("crtDt")));
        result.put("lastChgDt", localDateTime(row.get("lastChgDt")));
        result.put("templateId", longValue(row.get("templateId")));
        result.put("templateVersionId", longValue(row.get("templateVersionId")));
        result.put("totalCount", intValue(row.get("totalCount"), 0));
        result.put("successCount", intValue(row.get("successCount"), 0));
        result.put("failedCount", intValue(row.get("failedCount"), 0));
        result.put("result", parseJson(stringValue(row.get("resultJson"))));
        return result;
    }

    private Map<String, Object> findContract(Long contractId) {
        List<Map<String, Object>> rows = namedJdbc.queryForList(
            "SELECT D.CONTRACT_ID AS contractId, D.CONTRACT_REF AS contractRef, D.TEMPLATE_ID AS templateId, D.TEMPLATE_VERSION_ID AS templateVersionId, D.BATCH_ID AS batchId, " +
                "D.CONTRACT_TITLE AS contractTitle, D.CONTRACT_MESSAGE AS contractMessage, D.SEND_TYPE_CD AS sendTypeCd, D.SIGNING_FLOW_CD AS signingFlowCd, D.STATUS_CD AS statusCd, " +
                "D.CREATOR_USER_ID AS creatorUserId, U.USER_NM AS creatorNm, D.CRT_DT AS crtDt, D.SENT_DT AS sentDt, D.COMPLETED_DT AS completedDt, D.CANCELLED_DT AS cancelledDt, " +
                "D.CANCELLED_REASON AS cancelledReason, D.EXPIRES_DT AS expiresDt, D.CURRENT_SIGN_ORDER AS currentSignOrder, D.TOTAL_SIGNER_COUNT AS totalSignerCount, D.SENDER_NM AS senderNm, " +
                "D.SENDER_EMAIL AS senderEmail, D.SENDER_TELNO AS senderTelno, D.COMPANY_SNAPSHOT_JSON AS companySnapshotJson, D.TEMPLATE_SCHEMA_JSON AS templateSchemaJson, D.TEMPLATE_LAYOUT_JSON AS templateLayoutJson, " +
                "D.FINAL_HTML_DATA AS finalHtmlData, D.FINAL_FILE_ID AS finalFileId, T.TEMPLATE_NM AS templateNm, T.TEMPLATE_CODE AS templateCode, T.TEMPLATE_CATEGORY_CD AS templateCategoryCd " +
            "FROM CONTRACT_DOCUMENT D LEFT JOIN USERS U ON U.USER_ID = D.CREATOR_USER_ID LEFT JOIN CONTRACT_TEMPLATE T ON T.TEMPLATE_ID = D.TEMPLATE_ID WHERE D.CONTRACT_ID = :contractId",
            Map.of("contractId", contractId)
        );
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contract not found.");
        }
        Map<String, Object> contract = normalizeContractSummary(rows.get(0));
        contract.put("batchId", longValue(rows.get(0).get("batchId")));
        contract.put("templateVersionId", longValue(rows.get(0).get("templateVersionId")));
        contract.put("cancelledReason", stringValue(rows.get(0).get("cancelledReason")));
        contract.put("senderNm", stringValue(rows.get(0).get("senderNm")));
        contract.put("senderEmail", stringValue(rows.get(0).get("senderEmail")));
        contract.put("senderTelno", stringValue(rows.get(0).get("senderTelno")));
        contract.put("companySnapshot", parseJson(stringValue(rows.get(0).get("companySnapshotJson"))));
        contract.put("templateSchema", parseJson(stringValue(rows.get(0).get("templateSchemaJson"))));
        contract.put("templateLayout", parseJson(stringValue(rows.get(0).get("templateLayoutJson"))));
        contract.put("finalHtmlData", stringValue(rows.get(0).get("finalHtmlData")));
        contract.put("templateCode", stringValue(rows.get(0).get("templateCode")));
        return contract;
    }

    private List<Map<String, Object>> readSigners(Long contractId) {
        return namedJdbc.queryForList(
            "SELECT S.SIGNER_ID AS signerId, S.CONTRACT_ID AS contractId, S.SIGNER_ORDER AS signerOrder, S.SIGNER_ROLE_CD AS signerRoleCd, S.SIGNER_TYPE_CD AS signerTypeCd, " +
                "S.INTERNAL_USER_ID AS internalUserId, IU.USER_NM AS internalUserNm, S.SIGNER_NM AS signerNm, S.SIGNER_EMAIL AS signerEmail, S.SIGNER_TELNO AS signerTelno, " +
                "S.STATUS_CD AS statusCd, S.OPENED_DT AS openedDt, S.SIGNED_DT AS signedDt, S.DECLINED_REASON AS declinedReason, S.CLAIMED_NM AS claimedNm, S.CLAIMED_EMAIL AS claimedEmail, S.CLAIMED_TELNO AS claimedTelno " +
            "FROM CONTRACT_SIGNER S LEFT JOIN USERS IU ON IU.USER_ID = S.INTERNAL_USER_ID WHERE S.CONTRACT_ID = :contractId ORDER BY S.SIGNER_ORDER ASC, S.SIGNER_ID ASC",
            Map.of("contractId", contractId)
        ).stream().map(row -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("signerId", longValue(row.get("signerId")));
            item.put("contractId", longValue(row.get("contractId")));
            item.put("signerOrder", intValue(row.get("signerOrder"), 1));
            item.put("signerRoleCd", stringValue(row.get("signerRoleCd"), "signer"));
            item.put("signerTypeCd", stringValue(row.get("signerTypeCd"), "external"));
            item.put("internalUserId", stringValue(row.get("internalUserId")));
            item.put("internalUserNm", stringValue(row.get("internalUserNm")));
            item.put("signerNm", stringValue(row.get("signerNm")));
            item.put("signerEmail", stringValue(row.get("signerEmail")));
            item.put("signerTelno", stringValue(row.get("signerTelno")));
            item.put("statusCd", stringValue(row.get("statusCd"), "pending"));
            item.put("openedDt", localDateTime(row.get("openedDt")));
            item.put("signedDt", localDateTime(row.get("signedDt")));
            item.put("declinedReason", stringValue(row.get("declinedReason")));
            item.put("claimedNm", stringValue(row.get("claimedNm")));
            item.put("claimedEmail", stringValue(row.get("claimedEmail")));
            item.put("claimedTelno", stringValue(row.get("claimedTelno")));
            return item;
        }).toList();
    }

    private List<Map<String, Object>> pendingOrOpenedSigners(Long contractId) {
        return readSigners(contractId).stream()
            .filter(item -> PENDING_SIGNER_STATUSES.contains(stringValue(item.get("statusCd"))))
            .toList();
    }

    private String resolveSignerName(List<Map<String, Object>> signers, Long signerId) {
        if (signerId == null) {
            return "-";
        }
        return signers.stream()
            .filter(item -> Objects.equals(longValue(item.get("signerId")), signerId))
            .map(item -> stringValue(firstNonBlank(item.get("claimedNm"), item.get("signerNm")), "-"))
            .findFirst()
            .orElse("-");
    }

    private List<Map<String, Object>> readInvitations(Long contractId) {
        return namedJdbc.queryForList(
            "SELECT I.INVITATION_ID AS invitationId, I.CONTRACT_ID AS contractId, I.SIGNER_ID AS signerId, I.TOKEN_HINT AS tokenHint, I.CHANNEL_CD AS channelCd, " +
                "I.DELIVERY_STATUS_CD AS deliveryStatusCd, I.DELIVERY_NOTE AS deliveryNote, I.ACTIVE_YN AS activeYn, I.CRT_DT AS crtDt, I.SENT_DT AS sentDt, I.OPENED_DT AS openedDt, " +
                "I.COMPLETED_DT AS completedDt, I.EXPIRES_DT AS expiresDt, S.SIGNER_ORDER AS signerOrder, S.SIGNER_NM AS signerNm, S.SIGNER_EMAIL AS signerEmail, S.STATUS_CD AS signerStatusCd " +
            "FROM CONTRACT_INVITATION I JOIN CONTRACT_SIGNER S ON S.SIGNER_ID = I.SIGNER_ID WHERE I.CONTRACT_ID = :contractId ORDER BY I.INVITATION_ID DESC",
            Map.of("contractId", contractId)
        ).stream().map(row -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("invitationId", longValue(row.get("invitationId")));
            item.put("contractId", longValue(row.get("contractId")));
            item.put("signerId", longValue(row.get("signerId")));
            item.put("tokenHint", stringValue(row.get("tokenHint")));
            item.put("channelCd", stringValue(row.get("channelCd"), "remote"));
            item.put("deliveryStatusCd", stringValue(row.get("deliveryStatusCd"), "pending"));
            item.put("deliveryNote", stringValue(row.get("deliveryNote")));
            item.put("activeYn", stringValue(row.get("activeYn"), "Y"));
            item.put("crtDt", localDateTime(row.get("crtDt")));
            item.put("sentDt", localDateTime(row.get("sentDt")));
            item.put("openedDt", localDateTime(row.get("openedDt")));
            item.put("completedDt", localDateTime(row.get("completedDt")));
            item.put("expiresDt", localDateTime(row.get("expiresDt")));
            item.put("signerOrder", intValue(row.get("signerOrder"), 1));
            item.put("signerNm", stringValue(row.get("signerNm")));
            item.put("signerEmail", stringValue(row.get("signerEmail")));
            item.put("signerStatusCd", stringValue(row.get("signerStatusCd")));
            return item;
        }).toList();
    }

    private List<Map<String, Object>> readFieldValues(Long contractId) {
        return namedJdbc.queryForList(
            "SELECT FIELD_VALUE_ID AS fieldValueId, CONTRACT_ID AS contractId, FIELD_KEY AS fieldKey, FIELD_VALUE AS fieldValue, REPEAT_GROUP_KEY AS repeatGroupKey, ROW_INDEX AS rowIndex, SIGNER_ID AS signerId, LAST_CHG_DT AS lastChgDt " +
            "FROM CONTRACT_FIELD_VALUE WHERE CONTRACT_ID = :contractId ORDER BY FIELD_KEY ASC, ROW_INDEX ASC, FIELD_VALUE_ID ASC",
            Map.of("contractId", contractId)
        ).stream().map(row -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("fieldValueId", longValue(row.get("fieldValueId")));
            item.put("contractId", longValue(row.get("contractId")));
            item.put("fieldKey", stringValue(row.get("fieldKey")));
            item.put("fieldValue", stringValue(row.get("fieldValue")));
            item.put("repeatGroupKey", stringValue(row.get("repeatGroupKey")));
            item.put("rowIndex", intValue(row.get("rowIndex"), 0));
            item.put("signerId", longValue(row.get("signerId")));
            item.put("lastChgDt", localDateTime(row.get("lastChgDt")));
            return item;
        }).toList();
    }

    private List<Map<String, Object>> readAuditLogs(Long contractId) {
        return namedJdbc.queryForList(
            "SELECT AUDIT_ID AS auditId, CONTRACT_ID AS contractId, SIGNER_ID AS signerId, ACTION_CD AS actionCd, ACTION_MESSAGE AS actionMessage, ACTOR_TYPE_CD AS actorTypeCd, ACTOR_USER_ID AS actorUserId, ACTOR_NM AS actorNm, PAYLOAD_JSON AS payloadJson, CRT_DT AS crtDt " +
            "FROM CONTRACT_AUDIT_LOG WHERE CONTRACT_ID = :contractId ORDER BY AUDIT_ID DESC",
            Map.of("contractId", contractId)
        ).stream().map(row -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("auditId", longValue(row.get("auditId")));
            item.put("contractId", longValue(row.get("contractId")));
            item.put("signerId", longValue(row.get("signerId")));
            item.put("actionCd", stringValue(row.get("actionCd")));
            item.put("actionMessage", stringValue(row.get("actionMessage")));
            item.put("actorTypeCd", stringValue(row.get("actorTypeCd")));
            item.put("actorUserId", stringValue(row.get("actorUserId")));
            item.put("actorNm", stringValue(row.get("actorNm")));
            item.put("payload", parseJson(stringValue(row.get("payloadJson"))));
            item.put("crtDt", localDateTime(row.get("crtDt")));
            return item;
        }).toList();
    }

    private Map<String, Object> collapseFieldMap(List<Map<String, Object>> fieldValues) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map<String, Object> item : fieldValues) {
            String fieldKey = stringValue(item.get("fieldKey"));
            if (StringUtils.isBlank(fieldKey)) {
                continue;
            }
            if (intValue(item.get("rowIndex"), 0) > 0 || StringUtils.isNotBlank(stringValue(item.get("repeatGroupKey")))) {
                List<Map<String, Object>> values = asList(result.get(fieldKey));
                values.add(item);
                result.put(fieldKey, values);
            } else {
                result.put(fieldKey, item.get("fieldValue"));
            }
        }
        return result;
    }

    private Map<String, Object> hydrateContract(Map<String, Object> contract, String userId, boolean admin) {
        Long contractId = longValue(contract.get("contractId"));
        List<Map<String, Object>> signers = readSigners(contractId);
        List<Map<String, Object>> fieldValues = readFieldValues(contractId);
        Map<String, Object> response = new LinkedHashMap<>(contract);
        response.put("signers", signers);
        response.put("invitations", readInvitations(contractId));
        response.put("fieldValues", fieldValues);
        response.put("fieldMap", collapseFieldMap(fieldValues));
        response.put("auditLogs", readAuditLogs(contractId));
        response.put("finalFile", buildFileSummary(stringValue(contract.get("finalFileId"))));
        response.put("manageAllowed", admin || Objects.equals(contract.get("creatorUserId"), userId));
        return response;
    }

    private void ensureContractReadable(Map<String, Object> contract, String userId, boolean admin) {
        if (admin || Objects.equals(contract.get("creatorUserId"), userId)) {
            return;
        }
        boolean internalAccess = queryCount(
            "SELECT COUNT(*) FROM CONTRACT_SIGNER WHERE CONTRACT_ID = :contractId AND INTERNAL_USER_ID = :userId",
            Map.of("contractId", contract.get("contractId"), "userId", userId)
        ) > 0;
        if (!internalAccess) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have access to this contract.");
        }
    }

    private void ensureContractManageable(Map<String, Object> contract, String userId, boolean admin) {
        if (admin || Objects.equals(contract.get("creatorUserId"), userId)) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to manage this contract.");
    }

    private Map<String, Object> resolveCurrentVersion(Map<String, Object> template, List<Map<String, Object>> versions, boolean admin) {
        if (versions.isEmpty()) {
            return null;
        }
        if (admin) {
            return versions.get(0);
        }
        Long publishedVersionId = longValue(template.get("publishedVersionId"));
        return versions.stream()
            .filter(item -> Objects.equals(longValue(item.get("templateVersionId")), publishedVersionId))
            .findFirst()
            .orElse(null);
    }

    private Map<String, Object> resolveVersionForContract(Map<String, Object> template, Long requestedVersionId, boolean admin) {
        List<Map<String, Object>> versions = asList(readTemplate(longValue(template.get("templateId")), stringValue(template.get("crtUserId")), admin).get("versions"));
        if (requestedVersionId != null) {
            return versions.stream()
                .filter(item -> Objects.equals(longValue(item.get("templateVersionId")), requestedVersionId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Template version not found."));
        }
        Long publishedVersionId = longValue(template.get("publishedVersionId"));
        if (publishedVersionId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The template is not published yet.");
        }
        return versions.stream()
            .filter(item -> Objects.equals(longValue(item.get("templateVersionId")), publishedVersionId))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Published template version not found."));
    }

    private Long createOrUpdateVersion(Long templateId, Long templateVersionId, Map<String, Object> payload, String sourceFileId, String backgroundFileId, String userId, boolean existingTemplate) {
        List<Map<String, Object>> currentRows = templateVersionId == null ? List.of() : namedJdbc.queryForList(
            "SELECT TEMPLATE_VERSION_ID AS templateVersionId, VERSION_NO AS versionNo, VERSION_STATUS_CD AS versionStatusCd, VERSION_LABEL AS versionLabel, VERSION_NOTE AS versionNote, SOURCE_FILE_ID AS sourceFileId, BACKGROUND_FILE_ID AS backgroundFileId, SCHEMA_JSON AS schemaJson, LAYOUT_JSON AS layoutJson " +
            "FROM CONTRACT_TEMPLATE_VERSION WHERE TEMPLATE_VERSION_ID = :templateVersionId",
            Map.of("templateVersionId", templateVersionId)
        );
        Map<String, Object> current = currentRows.isEmpty() ? null : currentRows.get(0);
        boolean newVersion = !existingTemplate || current == null || "published".equalsIgnoreCase(stringValue(current.get("versionStatusCd")));
        String schemaJson = toJson(firstNonBlank(payload.get("schema"), payload.get("schemaJson"), parseJson(stringValue(current == null ? null : current.get("schemaJson"))), defaultSchema()));
        String layoutJson = toJson(firstNonBlank(payload.get("layout"), payload.get("layoutJson"), parseJson(stringValue(current == null ? null : current.get("layoutJson"))), defaultLayout()));

        if (newVersion) {
            int nextVersionNo = queryCount("SELECT COALESCE(MAX(VERSION_NO), 0) FROM CONTRACT_TEMPLATE_VERSION WHERE TEMPLATE_ID = :templateId", Map.of("templateId", templateId)) + 1;
            return namedJdbc.queryForObject(
                "INSERT INTO CONTRACT_TEMPLATE_VERSION (TEMPLATE_ID, VERSION_NO, VERSION_LABEL, VERSION_STATUS_CD, VERSION_NOTE, SOURCE_FILE_ID, BACKGROUND_FILE_ID, SCHEMA_JSON, LAYOUT_JSON, CRT_USER_ID, CRT_DT, LAST_CHG_DT) " +
                    "VALUES (:templateId, :versionNo, :versionLabel, :versionStatusCd, :versionNote, :sourceFileId, :backgroundFileId, :schemaJson, :layoutJson, :userId, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) RETURNING TEMPLATE_VERSION_ID",
                new MapSqlParameterSource()
                    .addValue("templateId", templateId)
                    .addValue("versionNo", nextVersionNo)
                    .addValue("versionLabel", stringValue(payload.get("versionLabel"), "v" + nextVersionNo + ".0"))
                    .addValue("versionStatusCd", normalizeEnum(payload.get("versionStatusCd"), Set.of("draft", "published", "archived"), "draft"))
                    .addValue("versionNote", blankToNull(payload.get("versionNote")))
                    .addValue("sourceFileId", blankToNull(firstNonBlank(sourceFileId, current == null ? null : current.get("sourceFileId"))))
                    .addValue("backgroundFileId", blankToNull(firstNonBlank(backgroundFileId, current == null ? null : current.get("backgroundFileId"))))
                    .addValue("schemaJson", schemaJson)
                    .addValue("layoutJson", layoutJson)
                    .addValue("userId", userId),
                Long.class
            );
        }

        namedJdbc.update(
            "UPDATE CONTRACT_TEMPLATE_VERSION SET VERSION_LABEL = :versionLabel, VERSION_NOTE = :versionNote, SOURCE_FILE_ID = :sourceFileId, BACKGROUND_FILE_ID = :backgroundFileId, " +
                "SCHEMA_JSON = :schemaJson, LAYOUT_JSON = :layoutJson, LAST_CHG_DT = CURRENT_TIMESTAMP WHERE TEMPLATE_VERSION_ID = :templateVersionId",
            new MapSqlParameterSource()
                .addValue("templateVersionId", templateVersionId)
                .addValue("versionLabel", stringValue(payload.get("versionLabel"), stringValue(current.get("versionLabel"))))
                .addValue("versionNote", blankToNull(firstNonBlank(payload.get("versionNote"), current.get("versionNote"))))
                .addValue("sourceFileId", blankToNull(firstNonBlank(sourceFileId, current.get("sourceFileId"))))
                .addValue("backgroundFileId", blankToNull(firstNonBlank(backgroundFileId, current.get("backgroundFileId"))))
                .addValue("schemaJson", schemaJson)
                .addValue("layoutJson", layoutJson)
        );
        return templateVersionId;
    }

    private String resolveUploadedFileId(MultipartFile file, String existingFileId, String folder) {
        if (file != null && !file.isEmpty()) {
            try {
                return fileUploadService.saveGeneratedFile(file.getBytes(), file.getOriginalFilename(), file.getContentType(), folder);
            } catch (IOException ex) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save uploaded file.", ex);
            }
        }
        return blankToNull(existingFileId);
    }

    private String normalizeTemplateCode(Object rawCode, String fallbackName) {
        String base = stringValue(firstNonBlank(rawCode, fallbackName), "CONTRACT_TEMPLATE");
        String normalized = base.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_").replaceAll("_+", "_");
        normalized = normalized.replaceAll("^_+", "").replaceAll("_+$", "");
        return normalized.isBlank() ? "CONTRACT_TEMPLATE" : normalized;
    }

    private boolean templateCodeExists(String templateCode, Long templateId) {
        String sql = "SELECT COUNT(*) FROM CONTRACT_TEMPLATE WHERE UPPER(TEMPLATE_CODE) = :templateCode" + (templateId == null ? "" : " AND TEMPLATE_ID <> :templateId");
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("templateCode", templateCode.toUpperCase(Locale.ROOT));
        if (templateId != null) {
            params.addValue("templateId", templateId);
        }
        return queryCount(sql, params) > 0;
    }

    private String ensureUniqueTemplateCode(String code, Long templateId) {
        String candidate = StringUtils.defaultIfBlank(code, "CONTRACT_TEMPLATE");
        String base = candidate;
        int suffix = 1;
        while (templateCodeExists(candidate, templateId)) {
            suffix++;
            candidate = base + "_" + suffix;
        }
        return candidate;
    }

    private Object defaultSchemaForRequest(String title) {
        return Map.of(
            "pages", List.of(Map.of("pageId", "page-1", "label", "1")),
            "fields", List.of(
                Map.ofEntries(
                    Map.entry("fieldId", "title"),
                    Map.entry("fieldKey", "title"),
                    Map.entry("label", title),
                    Map.entry("fieldTypeCd", "text"),
                    Map.entry("pageId", "page-1"),
                    Map.entry("x", 72),
                    Map.entry("y", 160),
                    Map.entry("width", 300),
                    Map.entry("height", 44),
                    Map.entry("requiredYn", "Y"),
                    Map.entry("assignmentCd", "creator")
                ),
                Map.ofEntries(
                    Map.entry("fieldId", "signature"),
                    Map.entry("fieldKey", "signature"),
                    Map.entry("label", "Signature"),
                    Map.entry("fieldTypeCd", "signature"),
                    Map.entry("pageId", "page-1"),
                    Map.entry("x", 72),
                    Map.entry("y", 320),
                    Map.entry("width", 280),
                    Map.entry("height", 120),
                    Map.entry("requiredYn", "Y"),
                    Map.entry("assignmentCd", "signer"),
                    Map.entry("signerOrder", 1)
                )
            )
        );
    }

    private Object defaultSchema() {
        return defaultSchemaForRequest("Contract");
    }

    private Object defaultLayout() {
        return Map.of("canvas", Map.of("width", 820, "height", 1160, "padding", 48, "backgroundColor", "#ffffff"));
    }

    private List<Map<String, Object>> normalizeSignerPayload(Object rawSigners) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (!(rawSigners instanceof Collection<?> items)) {
            return result;
        }
        int order = 1;
        for (Object item : items) {
            if (item instanceof Map<?, ?> map) {
                Map<String, Object> signer = new LinkedHashMap<>();
                signer.put("signerOrder", intValue(map.get("signerOrder"), order));
                signer.put("signerRoleCd", stringValue(map.get("signerRoleCd"), "signer"));
                signer.put("signerTypeCd", stringValue(map.get("signerTypeCd"), StringUtils.isNotBlank(stringValue(map.get("internalUserId"))) ? "internal" : "external"));
                signer.put("internalUserId", blankToNull(map.get("internalUserId")));
                signer.put("signerNm", blankToNull(firstNonBlank(map.get("signerNm"), map.get("name"))));
                signer.put("signerEmail", blankToNull(firstNonBlank(map.get("signerEmail"), map.get("email"))));
                signer.put("signerTelno", blankToNull(firstNonBlank(map.get("signerTelno"), map.get("tel"))));
                result.add(signer);
                order++;
            }
        }
        return result;
    }

    private List<Map<String, Object>> normalizeBatchRows(Object rawRows) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (!(rawRows instanceof Collection<?> items)) {
            return result;
        }
        for (Object item : items) {
            if (item instanceof Map<?, ?> map) {
                result.add(new LinkedHashMap<>((Map<String, Object>) map));
            }
        }
        return result;
    }

    private Map<String, Object> normalizeSingleSignerRow(Map<String, Object> row) {
        Map<String, Object> signer = new LinkedHashMap<>();
        signer.put("signerOrder", 1);
        signer.put("signerRoleCd", "signer");
        signer.put("signerTypeCd", StringUtils.isNotBlank(stringValue(row.get("internalUserId"))) ? "internal" : "external");
        signer.put("internalUserId", blankToNull(row.get("internalUserId")));
        signer.put("signerNm", blankToNull(firstNonBlank(row.get("signerNm"), row.get("name"), row.get("recipientName"))));
        signer.put("signerEmail", blankToNull(firstNonBlank(row.get("signerEmail"), row.get("email"), row.get("recipientEmail"))));
        signer.put("signerTelno", blankToNull(firstNonBlank(row.get("signerTelno"), row.get("tel"), row.get("recipientTelno"))));
        return signer;
    }

    private Map<String, Object> enrichInternalSigner(Map<String, Object> signer) {
        String internalUserId = stringValue(signer.get("internalUserId"));
        if (StringUtils.isBlank(internalUserId)) {
            return signer;
        }
        try {
            Map<String, Object> user = namedJdbc.queryForMap(
                "SELECT USER_ID AS userId, USER_NM AS userNm, USER_EMAIL AS userEmail, USER_TELNO AS userTelno FROM USERS WHERE USER_ID = :userId",
                Map.of("userId", internalUserId)
            );
            Map<String, Object> result = new LinkedHashMap<>(signer);
            result.put("signerNm", firstNonBlank(signer.get("signerNm"), user.get("userNm")));
            result.put("signerEmail", firstNonBlank(signer.get("signerEmail"), user.get("userEmail")));
            result.put("signerTelno", firstNonBlank(signer.get("signerTelno"), user.get("userTelno")));
            result.put("signerTypeCd", "internal");
            return result;
        } catch (EmptyResultDataAccessException ex) {
            return signer;
        }
    }

    private void persistContractSigners(Long contractId, List<Map<String, Object>> signers) {
        for (Map<String, Object> signer : signers) {
            Map<String, Object> resolved = enrichInternalSigner(signer);
            namedJdbc.update(
                "INSERT INTO CONTRACT_SIGNER (CONTRACT_ID, SIGNER_ORDER, SIGNER_ROLE_CD, SIGNER_TYPE_CD, INTERNAL_USER_ID, SIGNER_NM, SIGNER_EMAIL, SIGNER_TELNO, STATUS_CD, LAST_CHG_DT) " +
                    "VALUES (:contractId, :signerOrder, :signerRoleCd, :signerTypeCd, :internalUserId, :signerNm, :signerEmail, :signerTelno, 'pending', CURRENT_TIMESTAMP)",
                new MapSqlParameterSource()
                    .addValue("contractId", contractId)
                    .addValue("signerOrder", intValue(resolved.get("signerOrder"), 1))
                    .addValue("signerRoleCd", stringValue(resolved.get("signerRoleCd"), "signer"))
                    .addValue("signerTypeCd", stringValue(resolved.get("signerTypeCd"), "external"))
                    .addValue("internalUserId", blankToNull(resolved.get("internalUserId")))
                    .addValue("signerNm", requiredText(resolved.get("signerNm"), "Signer name is required."))
                    .addValue("signerEmail", blankToNull(resolved.get("signerEmail")))
                    .addValue("signerTelno", blankToNull(resolved.get("signerTelno")))
            );
        }
    }

    private void persistFieldValues(Long contractId, Long signerId, Object rawFieldValues, boolean replaceAll) {
        if (replaceAll && signerId == null) {
            namedJdbc.update("DELETE FROM CONTRACT_FIELD_VALUE WHERE CONTRACT_ID = :contractId", Map.of("contractId", contractId));
        }
        if (rawFieldValues == null) {
            return;
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        if (rawFieldValues instanceof Map<?, ?> fieldMap) {
            fieldMap.forEach((key, value) -> {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("fieldKey", String.valueOf(key));
                row.put("fieldValue", value);
                rows.add(row);
            });
        } else if (rawFieldValues instanceof Collection<?> items) {
            for (Object item : items) {
                if (item instanceof Map<?, ?> map) {
                    rows.add(new LinkedHashMap<>((Map<String, Object>) map));
                }
            }
        }

        for (Map<String, Object> row : rows) {
            String fieldKey = stringValue(row.get("fieldKey"));
            if (StringUtils.isBlank(fieldKey)) {
                continue;
            }
            Long targetSignerId = longValue(firstNonBlank(row.get("signerId"), signerId));
            if (!replaceAll) {
                namedJdbc.update(
                    "DELETE FROM CONTRACT_FIELD_VALUE WHERE CONTRACT_ID = :contractId AND FIELD_KEY = :fieldKey AND COALESCE(SIGNER_ID, 0) = COALESCE(:signerId, 0)",
                    new MapSqlParameterSource().addValue("contractId", contractId).addValue("fieldKey", fieldKey).addValue("signerId", targetSignerId)
                );
            }
            namedJdbc.update(
                "INSERT INTO CONTRACT_FIELD_VALUE (CONTRACT_ID, FIELD_KEY, FIELD_VALUE, REPEAT_GROUP_KEY, ROW_INDEX, SIGNER_ID, LAST_CHG_DT) VALUES (:contractId, :fieldKey, :fieldValue, :repeatGroupKey, :rowIndex, :signerId, CURRENT_TIMESTAMP)",
                new MapSqlParameterSource()
                    .addValue("contractId", contractId)
                    .addValue("fieldKey", fieldKey)
                    .addValue("fieldValue", row.get("fieldValue") instanceof String ? row.get("fieldValue") : toJson(row.get("fieldValue")))
                    .addValue("repeatGroupKey", blankToNull(row.get("repeatGroupKey")))
                    .addValue("rowIndex", intValue(row.get("rowIndex"), 0))
                    .addValue("signerId", targetSignerId)
            );
        }
    }

    private List<Map<String, Object>> issueLinks(Long contractId, List<Map<String, Object>> signers, String actorUserId, String deliveryStatus, String deliveryNote) {
        List<Map<String, Object>> links = new ArrayList<>();
        LocalDateTime expiresDt = localDateTime(findContract(contractId).get("expiresDt"));
        String channelCd = stringValue(findContract(contractId).get("sendTypeCd"), "remote");
        for (Map<String, Object> signer : signers) {
            Long signerId = longValue(signer.get("signerId"));
            if (signerId == null || FINAL_SIGNER_STATUSES.contains(stringValue(signer.get("statusCd")))) {
                continue;
            }
            namedJdbc.update(
                "UPDATE CONTRACT_INVITATION SET ACTIVE_YN = 'N', LAST_CHG_DT = CURRENT_TIMESTAMP WHERE CONTRACT_ID = :contractId AND SIGNER_ID = :signerId AND COALESCE(ACTIVE_YN, 'Y') = 'Y'",
                new MapSqlParameterSource().addValue("contractId", contractId).addValue("signerId", signerId)
            );
            String rawToken = generateRawToken();
            Long invitationId = namedJdbc.queryForObject(
                "INSERT INTO CONTRACT_INVITATION (CONTRACT_ID, SIGNER_ID, TOKEN_HASH, TOKEN_HINT, CHANNEL_CD, DELIVERY_STATUS_CD, DELIVERY_NOTE, ACTIVE_YN, CRT_DT, SENT_DT, EXPIRES_DT, LAST_CHG_DT) " +
                    "VALUES (:contractId, :signerId, :tokenHash, :tokenHint, :channelCd, :deliveryStatusCd, :deliveryNote, 'Y', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, :expiresDt, CURRENT_TIMESTAMP) RETURNING INVITATION_ID",
                new MapSqlParameterSource()
                    .addValue("contractId", contractId)
                    .addValue("signerId", signerId)
                    .addValue("tokenHash", sha256(rawToken))
                    .addValue("tokenHint", rawToken.substring(Math.max(0, rawToken.length() - 6)))
                    .addValue("channelCd", channelCd)
                    .addValue("deliveryStatusCd", deliveryStatus)
                    .addValue("deliveryNote", deliveryNote)
                    .addValue("expiresDt", expiresDt),
                Long.class
            );
            Map<String, Object> link = new LinkedHashMap<>();
            link.put("invitationId", invitationId);
            link.put("signerId", signerId);
            link.put("signerOrder", signer.get("signerOrder"));
            link.put("signerNm", signer.get("signerNm"));
            link.put("signerEmail", signer.get("signerEmail"));
            link.put("signerTelno", signer.get("signerTelno"));
            link.put("signPath", "/contract/sign/" + rawToken);
            link.put("signUrl", trimTrailingSlash(frontendUrl) + "/contract/sign/" + rawToken);
            link.put("channelCd", channelCd);
            link.put("expiresDt", expiresDt);
            links.add(link);
            if (StringUtils.isNotBlank(stringValue(signer.get("internalUserId")))) {
                sendNotification(stringValue(signer.get("internalUserId")), actorUserId, "CONTRACT_SENT", String.valueOf(contractId));
            }
        }
        return links;
    }

    private void refreshCurrentSignOrder(Long contractId) {
        Integer currentOrder = namedJdbc.queryForObject(
            "SELECT COALESCE(MIN(SIGNER_ORDER), 1) FROM CONTRACT_SIGNER WHERE CONTRACT_ID = :contractId AND STATUS_CD IN ('pending', 'opened')",
            Map.of("contractId", contractId),
            Integer.class
        );
        namedJdbc.update(
            "UPDATE CONTRACT_DOCUMENT SET CURRENT_SIGN_ORDER = :currentSignOrder, LAST_CHG_DT = CURRENT_TIMESTAMP WHERE CONTRACT_ID = :contractId",
            new MapSqlParameterSource().addValue("contractId", contractId).addValue("currentSignOrder", currentOrder == null ? 1 : currentOrder)
        );
    }

    private void addAudit(Long contractId, Long signerId, String actionCd, String actionMessage, String actorUserId, String actorTypeCd, Object actorNm, String payloadJson) {
        namedJdbc.update(
            "INSERT INTO CONTRACT_AUDIT_LOG (CONTRACT_ID, SIGNER_ID, ACTION_CD, ACTION_MESSAGE, ACTOR_TYPE_CD, ACTOR_USER_ID, ACTOR_NM, PAYLOAD_JSON, CRT_DT) " +
                "VALUES (:contractId, :signerId, :actionCd, :actionMessage, :actorTypeCd, :actorUserId, :actorNm, :payloadJson, CURRENT_TIMESTAMP)",
            new MapSqlParameterSource()
                .addValue("contractId", contractId)
                .addValue("signerId", signerId)
                .addValue("actionCd", actionCd)
                .addValue("actionMessage", actionMessage)
                .addValue("actorTypeCd", blankToNull(actorTypeCd))
                .addValue("actorUserId", blankToNull(actorUserId))
                .addValue("actorNm", blankToNull(actorNm))
                .addValue("payloadJson", payloadJson)
        );
    }

    private void notifyInternalSigners(Long contractId, String senderId, String alarmCode) {
        Set<String> receivers = readSigners(contractId).stream()
            .map(item -> stringValue(item.get("internalUserId")))
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        receivers.remove(senderId);
        for (String receiverId : receivers) {
            sendNotification(receiverId, senderId, alarmCode, String.valueOf(contractId));
        }
    }

    private void notifyAdmins(String alarmCode, String senderId, String pk) {
        List<String> admins = namedJdbc.queryForList("SELECT USER_ID FROM USERS WHERE USER_ROLE ILIKE '%ADMIN%'", Map.of(), String.class);
        for (String receiverId : admins) {
            if (!Objects.equals(receiverId, senderId)) {
                sendNotification(receiverId, senderId, alarmCode, pk);
            }
        }
    }

    private void sendNotification(String receiverId, String senderId, String alarmCode, String pk) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("receiverId", receiverId);
        payload.put("senderId", senderId);
        payload.put("alarmCode", alarmCode);
        payload.put("pk", pk);
        notificationService.sendNotification(payload);
    }

    private void expireOutdatedContracts() {
        namedJdbc.update(
            "UPDATE CONTRACT_DOCUMENT SET STATUS_CD = 'expired', LAST_CHG_DT = CURRENT_TIMESTAMP WHERE STATUS_CD IN ('draft', 'ready', 'sent', 'in_progress') AND EXPIRES_DT IS NOT NULL AND EXPIRES_DT < CURRENT_TIMESTAMP",
            Map.of()
        );
        namedJdbc.update(
            "UPDATE CONTRACT_SIGNER SET STATUS_CD = 'expired', LAST_CHG_DT = CURRENT_TIMESTAMP WHERE STATUS_CD IN ('pending', 'opened') AND CONTRACT_ID IN (SELECT CONTRACT_ID FROM CONTRACT_DOCUMENT WHERE STATUS_CD = 'expired')",
            Map.of()
        );
        namedJdbc.update(
            "UPDATE CONTRACT_INVITATION SET ACTIVE_YN = 'N', DELIVERY_STATUS_CD = CASE WHEN DELIVERY_STATUS_CD = 'completed' THEN DELIVERY_STATUS_CD ELSE 'expired' END, LAST_CHG_DT = CURRENT_TIMESTAMP WHERE COALESCE(ACTIVE_YN, 'Y') = 'Y' AND CONTRACT_ID IN (SELECT CONTRACT_ID FROM CONTRACT_DOCUMENT WHERE STATUS_CD = 'expired')",
            Map.of()
        );
    }

    private InvitationContext resolveInvitation(String token, boolean requireActive) {
        try {
            Map<String, Object> invitation = namedJdbc.queryForMap(
                "SELECT INVITATION_ID AS invitationId, CONTRACT_ID AS contractId, SIGNER_ID AS signerId, TOKEN_HINT AS tokenHint, CHANNEL_CD AS channelCd, DELIVERY_STATUS_CD AS deliveryStatusCd, " +
                    "DELIVERY_NOTE AS deliveryNote, ACTIVE_YN AS activeYn, CRT_DT AS crtDt, SENT_DT AS sentDt, OPENED_DT AS openedDt, COMPLETED_DT AS completedDt, EXPIRES_DT AS expiresDt " +
                "FROM CONTRACT_INVITATION WHERE TOKEN_HASH = :tokenHash ORDER BY INVITATION_ID DESC LIMIT 1",
                Map.of("tokenHash", sha256(token))
            );
            Map<String, Object> contract = findContract(longValue(invitation.get("contractId")));
            Map<String, Object> signer = readSigners(longValue(invitation.get("contractId"))).stream()
                .filter(item -> Objects.equals(longValue(item.get("signerId")), longValue(invitation.get("signerId"))))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Signer not found."));
            Map<String, Object> normalizedInvitation = readInvitations(longValue(invitation.get("contractId"))).stream()
                .filter(item -> Objects.equals(longValue(item.get("invitationId")), longValue(invitation.get("invitationId"))))
                .findFirst()
                .orElse(invitation);
            if (requireActive && !"Y".equalsIgnoreCase(stringValue(normalizedInvitation.get("activeYn"), "Y"))) {
                throw new ResponseStatusException(HttpStatus.GONE, "This invitation is no longer active.");
            }
            return new InvitationContext(longValue(contract.get("contractId")), longValue(signer.get("signerId")), longValue(normalizedInvitation.get("invitationId")), contract, signer, normalizedInvitation);
        } catch (EmptyResultDataAccessException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitation not found.");
        }
    }

    private void markOpened(InvitationContext context) {
        if (context.invitation.get("openedDt") == null) {
            namedJdbc.update("UPDATE CONTRACT_INVITATION SET OPENED_DT = CURRENT_TIMESTAMP, DELIVERY_STATUS_CD = 'opened', LAST_CHG_DT = CURRENT_TIMESTAMP WHERE INVITATION_ID = :invitationId", Map.of("invitationId", context.invitationId));
        }
        if (context.signer.get("openedDt") == null && PENDING_SIGNER_STATUSES.contains(stringValue(context.signer.get("statusCd")))) {
            namedJdbc.update("UPDATE CONTRACT_SIGNER SET OPENED_DT = CURRENT_TIMESTAMP, STATUS_CD = 'opened', LAST_CHG_DT = CURRENT_TIMESTAMP WHERE SIGNER_ID = :signerId AND STATUS_CD = 'pending'", Map.of("signerId", context.signerId));
        }
        if ("sent".equals(context.contract.get("statusCd"))) {
            namedJdbc.update("UPDATE CONTRACT_DOCUMENT SET STATUS_CD = 'in_progress', LAST_CHG_DT = CURRENT_TIMESTAMP WHERE CONTRACT_ID = :contractId", Map.of("contractId", context.contractId));
        }
    }

    private boolean isExpired(Map<String, Object> contract) {
        LocalDateTime expiresDt = localDateTime(contract.get("expiresDt"));
        return expiresDt != null && expiresDt.isBefore(LocalDateTime.now()) && !"completed".equals(contract.get("statusCd")) && !"cancelled".equals(contract.get("statusCd"));
    }

    private Map<String, Object> buildPublicResponse(InvitationContext context) {
        Map<String, Object> contract = hydrateContract(context.contract, stringValue(context.contract.get("creatorUserId")), true);
        boolean sequentialBlocked = "sequential".equals(contract.get("signingFlowCd"))
            && !Objects.equals(longValue(contract.get("currentSignOrder")), longValue(context.signer.get("signerOrder")))
            && !"signed".equals(context.signer.get("statusCd"));
        boolean canSubmit = "Y".equalsIgnoreCase(stringValue(context.invitation.get("activeYn"), "Y"))
            && !isExpired(contract)
            && !"completed".equals(contract.get("statusCd"))
            && !"cancelled".equals(contract.get("statusCd"))
            && !sequentialBlocked
            && !"signed".equals(context.signer.get("statusCd"));
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("contract", contract);
        response.put("signer", context.signer);
        response.put("invitation", context.invitation);
        response.put("canSubmit", canSubmit);
        response.put("downloadReady", StringUtils.isNotBlank(stringValue(contract.get("finalFileId"))));
        response.put(
            "blockedReason",
            canSubmit ? null : sequentialBlocked ? "This contract is waiting for a previous signer." : isExpired(contract) ? "This contract is expired." : "This contract is not submittable."
        );
        return response;
    }

    private void ensurePublicSubmitAllowed(InvitationContext context) {
        if (!"Y".equalsIgnoreCase(stringValue(context.invitation.get("activeYn"), "Y"))) {
            throw new ResponseStatusException(HttpStatus.GONE, "This invitation is no longer active.");
        }
        if (isExpired(context.contract)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The contract is expired.");
        }
        if ("completed".equals(context.contract.get("statusCd")) || "cancelled".equals(context.contract.get("statusCd"))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This contract is no longer signable.");
        }
        if ("sequential".equals(context.contract.get("signingFlowCd"))
            && !Objects.equals(longValue(context.contract.get("currentSignOrder")), longValue(context.signer.get("signerOrder")))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This contract is waiting for a previous signer.");
        }
    }

    private void completeContract(Long contractId) {
        Map<String, Object> contract = findContract(contractId);
        List<Map<String, Object>> signers = readSigners(contractId);
        List<Map<String, Object>> fieldValues = readFieldValues(contractId);
        String finalHtml = buildFinalHtml(contract, signers, fieldValues);
        String fileId;
        try {
            fileId = fileUploadService.saveGeneratedFile(
                pdfService.generatePdfFromHtml(finalHtml),
                stringValue(contract.get("contractRef"), "contract") + ".pdf",
                "application/pdf",
                FileFolderType.CONTRACT_PDF.toString()
            );
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate final PDF.", ex);
        }
        namedJdbc.update(
            "UPDATE CONTRACT_DOCUMENT SET STATUS_CD = 'completed', COMPLETED_DT = CURRENT_TIMESTAMP, FINAL_HTML_DATA = :finalHtmlData, FINAL_FILE_ID = :finalFileId, LAST_CHG_DT = CURRENT_TIMESTAMP WHERE CONTRACT_ID = :contractId",
            new MapSqlParameterSource().addValue("contractId", contractId).addValue("finalHtmlData", finalHtml).addValue("finalFileId", fileId)
        );
        addAudit(contractId, null, "completed", "All signers completed the contract", null, "system", contract.get("contractTitle"), null);
        sendNotification(stringValue(contract.get("creatorUserId")), stringValue(contract.get("creatorUserId")), "CONTRACT_COMPLETED", String.valueOf(contractId));
    }

    private String buildFinalHtml(Map<String, Object> contract, List<Map<String, Object>> signers, List<Map<String, Object>> fieldValues) {
        StringBuilder html = new StringBuilder();
        html.append("<html><head><meta charset=\"UTF-8\" /><style>body{font-family:'Malgun Gothic',sans-serif;padding:32px;}table{width:100%;border-collapse:collapse;margin-top:12px;}th,td{border:1px solid #d1d5db;padding:10px;text-align:left;}th{background:#f3f4f6;}</style></head><body>");
        html.append("<h1>").append(stringValue(contract.get("contractTitle"), "Contract")).append("</h1>");
        html.append("<p>Reference: ").append(stringValue(contract.get("contractRef"), "-")).append("</p>");
        html.append("<h2>Fields</h2><table><thead><tr><th>Field</th><th>Value</th><th>Signer</th></tr></thead><tbody>");
        for (Map<String, Object> fieldValue : fieldValues) {
            html.append("<tr><td>").append(stringValue(fieldValue.get("fieldKey"), "")).append("</td><td>")
                .append(stringValue(fieldValue.get("fieldValue"), "")).append("</td><td>")
                .append(resolveSignerName(signers, longValue(fieldValue.get("signerId")))).append("</td></tr>");
        }
        html.append("</tbody></table><h2>Signers</h2><table><thead><tr><th>Order</th><th>Name</th><th>Status</th></tr></thead><tbody>");
        for (Map<String, Object> signer : signers) {
            html.append("<tr><td>").append(intValue(signer.get("signerOrder"), 1)).append("</td><td>")
                .append(stringValue(firstNonBlank(signer.get("claimedNm"), signer.get("signerNm")), "-")).append("</td><td>")
                .append(stringValue(signer.get("statusCd"), "-")).append("</td></tr>");
        }
        html.append("</tbody></table></body></html>");
        return html.toString();
    }

    private Map<String, Object> buildCompanySnapshot(Map<String, Object> settings) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("companyName", settings.get("companyNm"));
        snapshot.put("senderName", settings.get("senderNm"));
        snapshot.put("senderEmail", settings.get("senderEmail"));
        snapshot.put("senderTelno", settings.get("senderTelno"));
        snapshot.put("sealFileId", settings.get("sealFileId"));
        return snapshot;
    }

    private byte[] readFileBytes(FileDetailVO fileDetail) {
        try {
            if ("local".equalsIgnoreCase(storageMode)) {
                return Files.readAllBytes(Path.of(fileDetail.getFilePath(), fileDetail.getSaveFileNm()));
            }
            String key = stringValue(fileDetailService.readFileDetailS3(fileDetail.getSaveFileNm()).get("key"));
            ResponseBytes<?> response = s3Client.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(key).build());
            return response.asByteArray();
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read file bytes.", ex);
        }
    }

    private Map<String, Object> buildFileSummary(String fileId) {
        if (StringUtils.isBlank(fileId)) {
            return null;
        }
        List<FileDetailVO> details = fileDetailService.readFileDetailList(fileId);
        if (details.isEmpty()) {
            return Map.of("fileId", fileId);
        }
        FileDetailVO detail = details.get(0);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("fileId", fileId);
        summary.put("orgnFileNm", detail.getOrgnFileNm());
        summary.put("saveFileNm", detail.getSaveFileNm());
        summary.put("downloadPath", "/rest/file/download/" + detail.getSaveFileNm());
        summary.put("fileMimeType", detail.getFileMimeType());
        return summary;
    }
}
