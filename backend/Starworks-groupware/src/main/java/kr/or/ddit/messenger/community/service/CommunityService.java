package kr.or.ddit.messenger.community.service;

import java.util.List;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

import kr.or.ddit.vo.CommunityMemberVO;
import kr.or.ddit.vo.CommunityVO;

public interface CommunityService {

    List<CommunityVO> getCommunities(String currentUserId, String keyword, String view, boolean manageableOnly, boolean admin);

    CommunityVO getCommunity(Long communityId, String currentUserId, boolean admin);

    CommunityVO createCommunity(CommunityVO community, String currentUserId, List<String> memberUserIds, List<String> operatorUserIds, MultipartFile iconFile, MultipartFile coverFile, boolean admin);

    CommunityVO updateCommunity(Long communityId, CommunityVO community, String currentUserId, List<String> operatorUserIds, MultipartFile iconFile, MultipartFile coverFile, boolean admin);

    void closeCommunity(Long communityId, String currentUserId, boolean admin);

    List<CommunityMemberVO> getMembers(Long communityId, String currentUserId, boolean admin, String statusCd);

    void addMembers(Long communityId, List<String> userIds, String currentUserId, boolean admin);

    void removeMember(Long communityId, String userId, String currentUserId, boolean admin);

    CommunityVO joinCommunity(Long communityId, String currentUserId, boolean admin);

    void leaveCommunity(Long communityId, String currentUserId, boolean admin);

    void updateMemberRole(Long communityId, String targetUserId, String roleCd, String currentUserId, boolean admin);

    List<CommunityMemberVO> getPendingMembers(Long communityId, String currentUserId, boolean admin);

    void approveMember(Long communityId, String targetUserId, String currentUserId, boolean admin);

    void rejectMember(Long communityId, String targetUserId, String currentUserId, boolean admin);

    void toggleFavorite(Long communityId, String currentUserId, boolean favorite);

    void saveOrder(String currentUserId, List<Long> communityIds);

    Map<String, Object> syncOrgCommunities(String currentUserId, boolean admin);
}
