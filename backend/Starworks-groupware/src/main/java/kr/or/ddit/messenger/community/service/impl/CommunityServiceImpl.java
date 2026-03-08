package kr.or.ddit.messenger.community.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import kr.or.ddit.comm.file.FileAttachable;
import kr.or.ddit.comm.file.FileFolderType;
import kr.or.ddit.comm.file.service.impl.FileUploadServiceImpl;
import kr.or.ddit.messenger.community.service.CommunityService;
import kr.or.ddit.mybatis.mapper.CommunityMapper;
import kr.or.ddit.vo.CommunityMemberVO;
import kr.or.ddit.vo.CommunityVO;
import kr.or.ddit.websocket.service.impl.NotificationServiceImpl;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommunityServiceImpl implements CommunityService {

    private final CommunityMapper communityMapper;
    private final NamedParameterJdbcTemplate namedJdbc;
    private final FileUploadServiceImpl fileUploadService;
    private final NotificationServiceImpl notificationService;

    @Override
    @Transactional(readOnly = true)
    public List<CommunityVO> getCommunities(String currentUserId, String keyword, String view, boolean manageableOnly, boolean admin) {
        return communityMapper.selectCommunities(currentUserId, keyword, normalizeView(view), manageableOnly, admin);
    }

    @Override
    @Transactional(readOnly = true)
    public CommunityVO getCommunity(Long communityId, String currentUserId, boolean admin) {
        CommunityVO community = communityMapper.selectCommunity(communityId, currentUserId, admin);
        if (community == null) {
            throw new IllegalArgumentException("커뮤니티를 찾을 수 없습니다.");
        }
        return community;
    }

    @Override
    @Transactional
    public CommunityVO createCommunity(
        CommunityVO community,
        String currentUserId,
        List<String> memberUserIds,
        List<String> operatorUserIds,
        MultipartFile iconFile,
        MultipartFile coverFile,
        boolean admin
    ) {
        if (community == null) {
            throw new IllegalArgumentException("커뮤니티 정보가 없습니다.");
        }

        normalizeCommunity(community, false);
        if ("org".equals(community.getCommunityTypeCd())) {
            throw new IllegalArgumentException("조직 커뮤니티는 수동으로 생성할 수 없습니다.");
        }

        community.setOwnerUserId(currentUserId);
        saveCommunityMedia(community, iconFile, coverFile);
        communityMapper.insertCommunity(community);

        communityMapper.upsertCommunityMember(buildMember(community.getCommunityId(), currentUserId, "owner", "active"));
        createPreferenceIfMissing(community.getCommunityId(), currentUserId);
        syncOperators(community.getCommunityId(), normalizeUserIds(operatorUserIds, null), currentUserId);

        for (String userId : normalizeUserIds(memberUserIds, null)) {
            if (!Objects.equals(userId, currentUserId)) {
                communityMapper.upsertCommunityMember(buildMember(community.getCommunityId(), userId, "member", "active"));
                createPreferenceIfMissing(community.getCommunityId(), userId);
                sendNotification(userId, currentUserId, "COMMUNITY_INVITED", String.valueOf(community.getCommunityId()));
            }
        }

        return getCommunity(community.getCommunityId(), currentUserId, admin);
    }

    @Override
    @Transactional
    public CommunityVO updateCommunity(
        Long communityId,
        CommunityVO community,
        String currentUserId,
        List<String> operatorUserIds,
        MultipartFile iconFile,
        MultipartFile coverFile,
        boolean admin
    ) {
        CommunityVO existing = getCommunity(communityId, currentUserId, admin);
        requireManage(existing, currentUserId, admin);
        if ("org".equals(existing.getCommunityTypeCd())) {
            throw new IllegalArgumentException("조직 커뮤니티는 수정할 수 없습니다.");
        }

        normalizeCommunity(community, true);
        community.setCommunityId(communityId);
        community.setOwnerUserId(existing.getOwnerUserId());
        community.setCommunityTypeCd(existing.getCommunityTypeCd());
        saveCommunityMedia(community, iconFile, coverFile);
        communityMapper.updateCommunity(community);

        if (operatorUserIds != null) {
            syncOperators(communityId, normalizeUserIds(operatorUserIds, null), existing.getOwnerUserId());
        }
        return getCommunity(communityId, currentUserId, admin);
    }

    @Override
    @Transactional
    public void closeCommunity(Long communityId, String currentUserId, boolean admin) {
        CommunityVO existing = getCommunity(communityId, currentUserId, admin);
        if ("org".equals(existing.getCommunityTypeCd())) {
            throw new IllegalArgumentException("조직 커뮤니티는 폐쇄할 수 없습니다.");
        }
        if (!admin && !Objects.equals(existing.getOwnerUserId(), currentUserId)) {
            throw new IllegalArgumentException("커뮤니티 폐쇄 권한이 없습니다.");
        }
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("communityId", communityId)
            .addValue("userId", currentUserId)
            .addValue("admin", admin);
        namedJdbc.update(
            "UPDATE COMMUNITY SET DEL_YN = 'Y', CLOSED_YN = 'Y', CLOSED_DT = CURRENT_TIMESTAMP, LAST_CHG_DT = CURRENT_TIMESTAMP " +
                "WHERE COMMUNITY_ID = :communityId AND (:admin = TRUE OR OWNER_USER_ID = :userId)",
            params
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommunityMemberVO> getMembers(Long communityId, String currentUserId, boolean admin, String statusCd) {
        CommunityVO community = getCommunity(communityId, currentUserId, admin);
        if (!admin && !isActiveMember(communityId, currentUserId) && !"public".equals(community.getVisibilityCd())) {
            throw new IllegalArgumentException("멤버 목록을 조회할 수 없습니다.");
        }
        return communityMapper.selectCommunityMembers(communityId, statusCd);
    }

    @Override
    @Transactional
    public void addMembers(Long communityId, List<String> userIds, String currentUserId, boolean admin) {
        CommunityVO existing = getCommunity(communityId, currentUserId, admin);
        requireManage(existing, currentUserId, admin);
        if ("org".equals(existing.getCommunityTypeCd())) {
            throw new IllegalArgumentException("조직 커뮤니티는 수동 초대를 지원하지 않습니다.");
        }

        for (String userId : normalizeUserIds(userIds, null)) {
            communityMapper.upsertCommunityMember(buildMember(communityId, userId, "member", "active"));
            createPreferenceIfMissing(communityId, userId);
            if (!Objects.equals(userId, currentUserId)) {
                sendNotification(userId, currentUserId, "COMMUNITY_INVITED", String.valueOf(communityId));
            }
        }
    }

    @Override
    @Transactional
    public void removeMember(Long communityId, String userId, String currentUserId, boolean admin) {
        CommunityVO existing = getCommunity(communityId, currentUserId, admin);
        requireManage(existing, currentUserId, admin);
        if ("org".equals(existing.getCommunityTypeCd())) {
            throw new IllegalArgumentException("조직 커뮤니티는 수동 멤버 변경을 지원하지 않습니다.");
        }
        if (Objects.equals(existing.getOwnerUserId(), userId)) {
            throw new IllegalArgumentException("커뮤니티 소유자는 제거할 수 없습니다.");
        }

        namedJdbc.update(
            "UPDATE COMMUNITY_MEMBER SET STATUS_CD = 'removed', LAST_CHG_DT = CURRENT_TIMESTAMP WHERE COMMUNITY_ID = :communityId AND USER_ID = :userId",
            new MapSqlParameterSource().addValue("communityId", communityId).addValue("userId", userId)
        );
        sendNotification(userId, currentUserId, "COMMUNITY_REMOVED", String.valueOf(communityId));
    }

    @Override
    @Transactional
    public CommunityVO joinCommunity(Long communityId, String currentUserId, boolean admin) {
        CommunityVO community = getCommunity(communityId, currentUserId, admin);
        if ("org".equals(community.getCommunityTypeCd())) {
            throw new IllegalArgumentException("조직 커뮤니티는 자동 가입됩니다.");
        }
        if ("private".equals(community.getVisibilityCd()) || "invite_only".equals(community.getJoinPolicyCd())) {
            throw new IllegalArgumentException("초대 전용 커뮤니티입니다.");
        }
        if (isActiveMember(communityId, currentUserId)) {
            return getCommunity(communityId, currentUserId, admin);
        }

        String statusCd = "approval".equals(community.getJoinPolicyCd()) ? "pending" : "active";
        communityMapper.upsertCommunityMember(buildMember(communityId, currentUserId, "member", statusCd));
        if ("active".equals(statusCd)) {
            createPreferenceIfMissing(communityId, currentUserId);
        } else {
            for (String managerId : findManagers(communityId)) {
                if (!Objects.equals(managerId, currentUserId)) {
                    sendNotification(managerId, currentUserId, "COMMUNITY_JOIN_REQUEST", String.valueOf(communityId));
                }
            }
        }
        return getCommunity(communityId, currentUserId, admin);
    }

    @Override
    @Transactional
    public void leaveCommunity(Long communityId, String currentUserId, boolean admin) {
        CommunityVO community = getCommunity(communityId, currentUserId, admin);
        if ("org".equals(community.getCommunityTypeCd())) {
            throw new IllegalArgumentException("조직 커뮤니티는 탈퇴할 수 없습니다.");
        }
        String roleCd = findMemberRole(communityId, currentUserId);
        if (!StringUtils.hasText(roleCd)) {
            throw new IllegalArgumentException("가입된 커뮤니티가 아닙니다.");
        }
        if ("owner".equals(roleCd)) {
            throw new IllegalArgumentException("커뮤니티 소유자는 탈퇴할 수 없습니다.");
        }
        namedJdbc.update(
            "UPDATE COMMUNITY_MEMBER SET STATUS_CD = 'left', LAST_CHG_DT = CURRENT_TIMESTAMP WHERE COMMUNITY_ID = :communityId AND USER_ID = :userId",
            new MapSqlParameterSource().addValue("communityId", communityId).addValue("userId", currentUserId)
        );
    }

    @Override
    @Transactional
    public void updateMemberRole(Long communityId, String targetUserId, String roleCd, String currentUserId, boolean admin) {
        CommunityVO community = getCommunity(communityId, currentUserId, admin);
        if ("org".equals(community.getCommunityTypeCd())) {
            throw new IllegalArgumentException("조직 커뮤니티는 역할을 변경할 수 없습니다.");
        }
        if (!admin && !Objects.equals(community.getOwnerUserId(), currentUserId)) {
            throw new IllegalArgumentException("운영자 권한 변경은 소유자만 가능합니다.");
        }
        if (Objects.equals(community.getOwnerUserId(), targetUserId)) {
            throw new IllegalArgumentException("커뮤니티 소유자 역할은 변경할 수 없습니다.");
        }
        String normalizedRole = normalizeRole(roleCd);
        namedJdbc.update(
            "UPDATE COMMUNITY_MEMBER SET ROLE_CD = :roleCd, STATUS_CD = 'active', LAST_CHG_DT = CURRENT_TIMESTAMP " +
                "WHERE COMMUNITY_ID = :communityId AND USER_ID = :userId",
            new MapSqlParameterSource().addValue("communityId", communityId).addValue("userId", targetUserId).addValue("roleCd", normalizedRole)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommunityMemberVO> getPendingMembers(Long communityId, String currentUserId, boolean admin) {
        CommunityVO community = getCommunity(communityId, currentUserId, admin);
        requireManage(community, currentUserId, admin);
        return communityMapper.selectCommunityMembers(communityId, "pending");
    }

    @Override
    @Transactional
    public void approveMember(Long communityId, String targetUserId, String currentUserId, boolean admin) {
        CommunityVO community = getCommunity(communityId, currentUserId, admin);
        requireManage(community, currentUserId, admin);
        namedJdbc.update(
            "UPDATE COMMUNITY_MEMBER SET STATUS_CD = 'active', LAST_CHG_DT = CURRENT_TIMESTAMP WHERE COMMUNITY_ID = :communityId AND USER_ID = :userId AND STATUS_CD = 'pending'",
            new MapSqlParameterSource().addValue("communityId", communityId).addValue("userId", targetUserId)
        );
        createPreferenceIfMissing(communityId, targetUserId);
        sendNotification(targetUserId, currentUserId, "COMMUNITY_JOIN_APPROVED", String.valueOf(communityId));
    }

    @Override
    @Transactional
    public void rejectMember(Long communityId, String targetUserId, String currentUserId, boolean admin) {
        CommunityVO community = getCommunity(communityId, currentUserId, admin);
        requireManage(community, currentUserId, admin);
        namedJdbc.update(
            "UPDATE COMMUNITY_MEMBER SET STATUS_CD = 'rejected', LAST_CHG_DT = CURRENT_TIMESTAMP WHERE COMMUNITY_ID = :communityId AND USER_ID = :userId AND STATUS_CD = 'pending'",
            new MapSqlParameterSource().addValue("communityId", communityId).addValue("userId", targetUserId)
        );
        sendNotification(targetUserId, currentUserId, "COMMUNITY_JOIN_REJECTED", String.valueOf(communityId));
    }

    @Override
    @Transactional
    public void toggleFavorite(Long communityId, String currentUserId, boolean favorite) {
        ensureActiveMembership(communityId, currentUserId);
        createPreferenceIfMissing(communityId, currentUserId);
        namedJdbc.update(
            "UPDATE COMMUNITY_USER_PREF SET FAVORITE_YN = :favoriteYn, LAST_CHG_DT = CURRENT_TIMESTAMP WHERE COMMUNITY_ID = :communityId AND USER_ID = :userId",
            new MapSqlParameterSource()
                .addValue("communityId", communityId)
                .addValue("userId", currentUserId)
                .addValue("favoriteYn", favorite ? "Y" : "N")
        );
    }

    @Override
    @Transactional
    public void saveOrder(String currentUserId, List<Long> communityIds) {
        int order = 1;
        for (Long communityId : normalizeCommunityIds(communityIds)) {
            if (!isActiveMember(communityId, currentUserId) || isOrgCommunity(communityId)) {
                continue;
            }
            createPreferenceIfMissing(communityId, currentUserId);
            namedJdbc.update(
                "UPDATE COMMUNITY_USER_PREF SET SORT_ORDR = :sortOrdr, LAST_CHG_DT = CURRENT_TIMESTAMP WHERE COMMUNITY_ID = :communityId AND USER_ID = :userId",
                new MapSqlParameterSource()
                    .addValue("communityId", communityId)
                    .addValue("userId", currentUserId)
                    .addValue("sortOrdr", order++)
            );
        }
    }

    @Override
    @Transactional
    public Map<String, Object> syncOrgCommunities(String currentUserId, boolean admin) {
        if (!admin) {
            throw new IllegalArgumentException("조직 커뮤니티 동기화 권한이 없습니다.");
        }
        return syncOrgCommunitiesInternal();
    }

    private void normalizeCommunity(CommunityVO community, boolean update) {
        if (!update && (!StringUtils.hasText(community.getCommunityNm()) || !StringUtils.hasText(community.getCommunityDesc()))) {
            throw new IllegalArgumentException("커뮤니티명과 소개는 필수입니다.");
        }
        community.setCommunityTypeCd(normalizeType(community.getCommunityTypeCd()));
        community.setVisibilityCd(normalizeVisibility(community.getVisibilityCd(), community.getCommunityTypeCd()));
        community.setJoinPolicyCd(normalizeJoinPolicy(community.getJoinPolicyCd(), community.getVisibilityCd(), community.getCommunityTypeCd()));
        community.setClosedYn("Y".equalsIgnoreCase(community.getClosedYn()) ? "Y" : "N");
        if (!StringUtils.hasText(community.getIntroText())) {
            community.setIntroText(community.getCommunityDesc());
        }
    }

    private String normalizeType(String value) {
        String normalized = safeLower(value);
        return switch (normalized) {
            case "notice", "org" -> normalized;
            default -> "general";
        };
    }

    private String normalizeVisibility(String value, String type) {
        if ("org".equals(type)) {
            return "org";
        }
        String normalized = safeLower(value);
        return switch (normalized) {
            case "public", "private" -> normalized;
            default -> "private";
        };
    }

    private String normalizeJoinPolicy(String value, String visibility, String type) {
        if ("org".equals(type)) {
            return "auto";
        }
        if ("private".equals(visibility)) {
            return "invite_only";
        }
        String normalized = safeLower(value);
        return switch (normalized) {
            case "approval", "instant" -> normalized;
            default -> "instant";
        };
    }

    private String normalizeView(String view) {
        String normalized = safeLower(view);
        return switch (normalized) {
            case "discover", "favorites" -> normalized;
            default -> "joined";
        };
    }

    private String normalizeRole(String roleCd) {
        String normalized = safeLower(roleCd);
        return switch (normalized) {
            case "owner", "operator" -> normalized;
            default -> "member";
        };
    }

    private void saveCommunityMedia(CommunityVO community, MultipartFile iconFile, MultipartFile coverFile) {
        if (iconFile != null && !iconFile.isEmpty()) {
            CommunityUploadTarget iconTarget = new CommunityUploadTarget(iconFile);
            fileUploadService.saveFileS3(iconTarget, FileFolderType.COMMUNITY.toString() + "/icon");
            community.setIconFileId(iconTarget.fileId);
        }
        if (coverFile != null && !coverFile.isEmpty()) {
            CommunityUploadTarget coverTarget = new CommunityUploadTarget(coverFile);
            fileUploadService.saveFileS3(coverTarget, FileFolderType.COMMUNITY.toString() + "/cover");
            community.setCoverFileId(coverTarget.fileId);
        }
    }

    private void syncOperators(Long communityId, List<String> operatorUserIds, String ownerUserId) {
        namedJdbc.update(
            "UPDATE COMMUNITY_MEMBER SET ROLE_CD = 'member', LAST_CHG_DT = CURRENT_TIMESTAMP " +
                "WHERE COMMUNITY_ID = :communityId AND ROLE_CD = 'operator' AND USER_ID <> :ownerUserId",
            new MapSqlParameterSource().addValue("communityId", communityId).addValue("ownerUserId", ownerUserId)
        );
        for (String userId : operatorUserIds) {
            if (Objects.equals(userId, ownerUserId)) {
                continue;
            }
            communityMapper.upsertCommunityMember(buildMember(communityId, userId, "operator", "active"));
            createPreferenceIfMissing(communityId, userId);
        }
    }

    private CommunityMemberVO buildMember(Long communityId, String userId, String roleCd, String statusCd) {
        CommunityMemberVO member = new CommunityMemberVO();
        member.setCommunityId(communityId);
        member.setUserId(userId);
        member.setRoleCd(roleCd);
        member.setStatusCd(statusCd);
        return member;
    }

    private void requireManage(CommunityVO community, String currentUserId, boolean admin) {
        if (community == null) {
            throw new IllegalArgumentException("커뮤니티를 찾을 수 없습니다.");
        }
        if (admin) {
            return;
        }
        String roleCd = findMemberRole(community.getCommunityId(), currentUserId);
        if (!"owner".equals(roleCd) && !"operator".equals(roleCd)) {
            throw new IllegalArgumentException("커뮤니티 관리 권한이 없습니다.");
        }
    }

    private void ensureActiveMembership(Long communityId, String userId) {
        if (!isActiveMember(communityId, userId)) {
            throw new IllegalArgumentException("가입된 커뮤니티가 아닙니다.");
        }
    }

    private boolean isActiveMember(Long communityId, String userId) {
        return communityMapper.existsActiveMember(communityId, userId);
    }

    private boolean isOrgCommunity(Long communityId) {
        Integer count = namedJdbc.queryForObject(
            "SELECT COUNT(*) FROM COMMUNITY WHERE COMMUNITY_ID = :communityId AND COMMUNITY_TYPE_CD = 'org'",
            new MapSqlParameterSource().addValue("communityId", communityId),
            Integer.class
        );
        return count != null && count > 0;
    }

    private String findMemberRole(Long communityId, String userId) {
        try {
            return namedJdbc.queryForObject(
                "SELECT ROLE_CD FROM COMMUNITY_MEMBER WHERE COMMUNITY_ID = :communityId AND USER_ID = :userId AND STATUS_CD = 'active'",
                new MapSqlParameterSource().addValue("communityId", communityId).addValue("userId", userId),
                String.class
            );
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private void createPreferenceIfMissing(Long communityId, String userId) {
        Integer nextOrder = namedJdbc.queryForObject(
            "SELECT COALESCE(MAX(SORT_ORDR), 0) + 1 FROM COMMUNITY_USER_PREF WHERE USER_ID = :userId",
            new MapSqlParameterSource().addValue("userId", userId),
            Integer.class
        );
        namedJdbc.update(
            "INSERT INTO COMMUNITY_USER_PREF (COMMUNITY_ID, USER_ID, FAVORITE_YN, SORT_ORDR, CRT_DT, LAST_CHG_DT) " +
                "VALUES (:communityId, :userId, 'N', :sortOrdr, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) " +
                "ON CONFLICT (COMMUNITY_ID, USER_ID) DO NOTHING",
            new MapSqlParameterSource()
                .addValue("communityId", communityId)
                .addValue("userId", userId)
                .addValue("sortOrdr", nextOrder == null ? 9999 : nextOrder)
        );
    }

    private List<String> findManagers(Long communityId) {
        return namedJdbc.queryForList(
            "SELECT USER_ID FROM COMMUNITY_MEMBER WHERE COMMUNITY_ID = :communityId AND STATUS_CD = 'active' AND ROLE_CD IN ('owner', 'operator') ORDER BY ROLE_CD, USER_ID",
            new MapSqlParameterSource().addValue("communityId", communityId),
            String.class
        );
    }

    private void sendNotification(String receiverId, String senderId, String alarmCode, String pk) {
        if (!StringUtils.hasText(receiverId)) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("receiverId", receiverId);
        payload.put("senderId", senderId);
        payload.put("alarmCode", alarmCode);
        payload.put("pk", pk);
        notificationService.sendNotification(payload);
    }

    private List<String> normalizeUserIds(List<String> userIds, String requiredUserId) {
        Set<String> normalized = new LinkedHashSet<>();
        if (StringUtils.hasText(requiredUserId)) {
            normalized.add(requiredUserId);
        }
        if (userIds != null) {
            for (String userId : userIds) {
                if (StringUtils.hasText(userId)) {
                    normalized.add(userId.trim());
                }
            }
        }
        return new ArrayList<>(normalized);
    }

    private List<Long> normalizeCommunityIds(List<Long> communityIds) {
        Set<Long> normalized = new LinkedHashSet<>();
        if (communityIds != null) {
            for (Long communityId : communityIds) {
                if (communityId != null) {
                    normalized.add(communityId);
                }
            }
        }
        return new ArrayList<>(normalized);
    }

    private String safeLower(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private Map<String, Object> syncOrgCommunitiesInternal() {
        int created = namedJdbc.update(
            "INSERT INTO COMMUNITY (COMMUNITY_NM, COMMUNITY_DESC, COMMUNITY_TYPE_CD, OWNER_USER_ID, DEL_YN, CRT_DT, LAST_CHG_DT, VISIBILITY_CD, JOIN_POLICY_CD, DEPT_ID, INTRO_TEXT, POST_TEMPLATE_HTML, CLOSED_YN) " +
                "SELECT D.DEPT_NM, D.DEPT_NM || ' 조직 커뮤니티', 'org', NULL, 'N', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'org', 'auto', D.DEPT_ID, D.DEPT_NM || ' 조직 커뮤니티', " +
                "       '<p><strong>' || D.DEPT_NM || '</strong> 공지 양식</p>', 'N' " +
                "FROM DEPARTMENT D WHERE COALESCE(D.USE_YN, 'N') = 'Y' " +
                "AND NOT EXISTS (SELECT 1 FROM COMMUNITY C WHERE C.COMMUNITY_TYPE_CD = 'org' AND C.DEPT_ID = D.DEPT_ID)",
            new MapSqlParameterSource()
        );

        int updated = namedJdbc.update(
            "UPDATE COMMUNITY C SET COMMUNITY_NM = D.DEPT_NM, COMMUNITY_DESC = D.DEPT_NM || ' 조직 커뮤니티', INTRO_TEXT = D.DEPT_NM || ' 조직 커뮤니티', " +
                "VISIBILITY_CD = 'org', JOIN_POLICY_CD = 'auto', DEL_YN = 'N', CLOSED_YN = 'N', CLOSED_DT = NULL, LAST_CHG_DT = CURRENT_TIMESTAMP " +
                "FROM DEPARTMENT D WHERE C.COMMUNITY_TYPE_CD = 'org' AND C.DEPT_ID = D.DEPT_ID AND COALESCE(D.USE_YN, 'N') = 'Y'",
            new MapSqlParameterSource()
        );

        int closed = namedJdbc.update(
            "UPDATE COMMUNITY C SET DEL_YN = 'Y', CLOSED_YN = 'Y', CLOSED_DT = COALESCE(CLOSED_DT, CURRENT_TIMESTAMP), LAST_CHG_DT = CURRENT_TIMESTAMP " +
                "WHERE C.COMMUNITY_TYPE_CD = 'org' AND C.DEPT_ID IS NOT NULL " +
                "AND NOT EXISTS (SELECT 1 FROM DEPARTMENT D WHERE D.DEPT_ID = C.DEPT_ID AND COALESCE(D.USE_YN, 'N') = 'Y')",
            new MapSqlParameterSource()
        );

        int activatedMembers = namedJdbc.update(
            "INSERT INTO COMMUNITY_MEMBER (COMMUNITY_ID, USER_ID, ROLE_CD, STATUS_CD, CRT_DT, LAST_CHG_DT) " +
                "SELECT C.COMMUNITY_ID, U.USER_ID, 'member', 'active', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP " +
                "FROM COMMUNITY C JOIN USERS U ON U.DEPT_ID = C.DEPT_ID " +
                "WHERE C.COMMUNITY_TYPE_CD = 'org' AND COALESCE(C.DEL_YN, 'N') = 'N' AND COALESCE(C.CLOSED_YN, 'N') = 'N' AND COALESCE(U.RSGNTN_YN, 'N') = 'N' " +
                "ON CONFLICT (COMMUNITY_ID, USER_ID) DO UPDATE SET ROLE_CD = 'member', STATUS_CD = 'active', LAST_CHG_DT = CURRENT_TIMESTAMP",
            new MapSqlParameterSource()
        );

        int removedMembers = namedJdbc.update(
            "UPDATE COMMUNITY_MEMBER CM SET STATUS_CD = 'removed', LAST_CHG_DT = CURRENT_TIMESTAMP " +
                "FROM COMMUNITY C " +
                "WHERE CM.COMMUNITY_ID = C.COMMUNITY_ID AND C.COMMUNITY_TYPE_CD = 'org' " +
                "AND NOT EXISTS (" +
                    "SELECT 1 FROM USERS U WHERE U.USER_ID = CM.USER_ID " +
                    "AND COALESCE(U.RSGNTN_YN, 'N') = 'N' AND U.DEPT_ID IS NOT DISTINCT FROM C.DEPT_ID" +
                ") AND CM.STATUS_CD <> 'removed'",
            new MapSqlParameterSource()
        );

        int prefInserted = namedJdbc.update(
            "INSERT INTO COMMUNITY_USER_PREF (COMMUNITY_ID, USER_ID, FAVORITE_YN, SORT_ORDR, CRT_DT, LAST_CHG_DT) " +
                "SELECT CM.COMMUNITY_ID, CM.USER_ID, 'N', ROW_NUMBER() OVER (PARTITION BY CM.USER_ID ORDER BY D.SORT_NUM, C.COMMUNITY_NM, C.COMMUNITY_ID), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP " +
                "FROM COMMUNITY_MEMBER CM JOIN COMMUNITY C ON C.COMMUNITY_ID = CM.COMMUNITY_ID " +
                "LEFT JOIN DEPARTMENT D ON D.DEPT_ID = C.DEPT_ID " +
                "WHERE CM.STATUS_CD = 'active' AND C.COMMUNITY_TYPE_CD = 'org' " +
                "ON CONFLICT (COMMUNITY_ID, USER_ID) DO NOTHING",
            new MapSqlParameterSource()
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("created", created);
        result.put("updated", updated);
        result.put("closed", closed);
        result.put("activatedMembers", activatedMembers);
        result.put("removedMembers", removedMembers);
        result.put("preferenceRows", prefInserted);
        return result;
    }

    private static class CommunityUploadTarget implements FileAttachable {
        private final List<MultipartFile> fileList;
        private String fileId;

        private CommunityUploadTarget(MultipartFile file) {
            this.fileList = List.of(file);
        }

        @Override
        public List<MultipartFile> getFileList() {
            return fileList;
        }

        @Override
        public void setFileId(String fileId) {
            this.fileId = fileId;
        }
    }
}
