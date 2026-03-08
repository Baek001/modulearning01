package kr.or.ddit.mybatis.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import kr.or.ddit.vo.CommunityMemberVO;
import kr.or.ddit.vo.CommunityVO;

@Mapper
public interface CommunityMapper {

    List<CommunityVO> selectCommunities(
        @Param("userId") String userId,
        @Param("keyword") String keyword,
        @Param("view") String view,
        @Param("manageableOnly") boolean manageableOnly,
        @Param("admin") boolean admin
    );

    CommunityVO selectCommunity(
        @Param("communityId") Long communityId,
        @Param("userId") String userId,
        @Param("admin") boolean admin
    );

    int insertCommunity(CommunityVO community);

    int updateCommunity(CommunityVO community);

    int deleteCommunity(@Param("communityId") Long communityId, @Param("userId") String userId);

    List<CommunityMemberVO> selectCommunityMembers(@Param("communityId") Long communityId, @Param("statusCd") String statusCd);

    int upsertCommunityMember(CommunityMemberVO member);

    int removeCommunityMember(@Param("communityId") Long communityId, @Param("userId") String userId);

    boolean existsActiveMember(@Param("communityId") Long communityId, @Param("userId") String userId);
}
