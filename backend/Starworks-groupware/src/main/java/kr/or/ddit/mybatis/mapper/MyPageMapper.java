package kr.or.ddit.mybatis.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import kr.or.ddit.vo.UsersVO;

@Mapper
public interface MyPageMapper {

    int updateUserInfo(UsersVO user);

    int updateUserPassword(@Param("userId") String userId, @Param("encodedPassword") String encodedPassword);
}
