package kr.or.ddit.mybatis.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import kr.or.ddit.vo.MessengerRoomVO;

@Mapper
public interface MessengerRoomMapper {

    int updateMessengerRoomName(@Param("msgrId") String msgrId, @Param("msgrNm") String msgrNm);

    int updatePinnedMessage(@Param("msgrId") String msgrId, @Param("msgContId") String msgContId, @Param("userId") String userId);

    int clearPinnedMessage(@Param("msgrId") String msgrId);

    List<MessengerRoomVO> selectMyRooms(@Param("userId") String userId);

    MessengerRoomVO selectMyRoom(@Param("msgrId") String msgrId, @Param("userId") String userId);

    MessengerRoomVO findPrivateRoom(@Param("userId1") String userId1, @Param("userId2") String userId2);

    MessengerRoomVO findSelfRoom(@Param("userId") String userId);

    int insertMessengerRoom(MessengerRoomVO mesRoom);

    List<MessengerRoomVO> selectMessengerRoomList();

    MessengerRoomVO selectMessengerRoom(String msgrId);

    int deleteMessengerRoom(String msgrId);
}
