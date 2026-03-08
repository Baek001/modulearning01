package kr.or.ddit.mybatis.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MessengerReadMapper {

	/**
     * 특정 채팅방의 읽지 않은 모든 메시지에 대해 MESSENGER_READ 기록을 추가합니다.
     * @param msgrId 채팅방 ID
     * @param userId 현재 사용자 ID
     * @return INSERT된 레코드 수
     */
    int insertReadRecords(@Param("msgrId") String msgrId, @Param("userId") String userId);

}
