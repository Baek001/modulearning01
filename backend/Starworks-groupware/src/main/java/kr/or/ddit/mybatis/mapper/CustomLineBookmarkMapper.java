package kr.or.ddit.mybatis.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import kr.or.ddit.vo.CustomLineBookmarkVO;

@Mapper
public interface CustomLineBookmarkMapper {

    int insertCustomLineBookmark(CustomLineBookmarkVO custLineBookmark);

    List<CustomLineBookmarkVO> selectCustomLineBookmarkList(String userId);

    int deleteCustomLineBookmark(@Param("userId") String userId, @Param("cstmLineBmNm") String cstmLineBmNm);
}
