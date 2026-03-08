package kr.or.ddit.mybatis.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import kr.or.ddit.vo.AuthorizationTempVO;

@Mapper
public interface AuthorizationTempMapper {

    List<AuthorizationTempVO> selectAuthTempList(String loginId);

    AuthorizationTempVO selectAuthTemp(@Param("atrzTempSqn") String atrzTempSqn);

    int insertAuthorizationTemp(AuthorizationTempVO authTemp);

    int updateAuthorizationTemp(AuthorizationTempVO authTemp);

    int deleteAuthTemp(String atrzTempSqn);
}
