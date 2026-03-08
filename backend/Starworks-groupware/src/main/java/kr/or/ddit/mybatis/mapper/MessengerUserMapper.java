package kr.or.ddit.mybatis.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import kr.or.ddit.vo.MessengerParticipantVO;
import kr.or.ddit.vo.MessengerUserVO;

@Mapper
public interface MessengerUserMapper {

    List<MessengerParticipantVO> selectRoomParticipants(@Param("msgrId") String msgrId, @Param("userId") String userId);

    int insertRoomMembers(@Param("msgrId") String msgrId, @Param("userIds") List<String> userIds, @Param("roleCd") String roleCd);

    int upsertRoomMember(@Param("msgrId") String msgrId, @Param("userId") String userId, @Param("roleCd") String roleCd);

    int countRoomUsers(String msgrId);

    int updateLeftTime(@Param("userId") String userId, @Param("msgrId") String msgrId);

    int updateNotify(@Param("userId") String userId, @Param("msgrId") String msgrId, @Param("notifyYn") String notifyYn);

    MessengerUserVO selectRoomMembership(@Param("msgrId") String msgrId, @Param("userId") String userId);

    String selectOwnerUserId(@Param("msgrId") String msgrId);

    int insertMessengerUser(MessengerUserVO mesUser);

    List<MessengerUserVO> selectMessengerUserList();

    MessengerUserVO selectMessengerUser(String userId);

    int deleteMessengerUser(String userId);
}
