package kr.or.ddit.mybatis.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import kr.or.ddit.vo.MessengerContentVO;

@Mapper
public interface MessengerContentMapper {

    List<MessengerContentVO> selectMessengerContentByRoomId(@Param("msgrId") String msgrId, @Param("userId") String userId);

    List<MessengerContentVO> searchMessengerContentByRoomId(@Param("msgrId") String msgrId, @Param("userId") String userId, @Param("keyword") String keyword);

    List<MessengerContentVO> selectMessengerContentForExport(@Param("msgrId") String msgrId, @Param("userId") String userId);

    MessengerContentVO selectMessengerContent(@Param("msgContId") String msgContId, @Param("userId") String userId);

    int updateReadStatus(String msgContId);

    int insertMessengerContent(MessengerContentVO mesContent);

    List<MessengerContentVO> selectMessengerContentList();

    int softDeleteMessengerContent(@Param("msgContId") String msgContId, @Param("userId") String userId);

    int deleteMessengerContent(String msgContId);
}
