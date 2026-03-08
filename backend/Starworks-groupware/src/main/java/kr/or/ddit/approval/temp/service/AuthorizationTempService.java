package kr.or.ddit.approval.temp.service;

import java.util.List;

import kr.or.ddit.vo.AuthorizationTempVO;

public interface AuthorizationTempService {

    List<AuthorizationTempVO> readAuthTempList();

    AuthorizationTempVO readAuthTemp(String atrzTempSqn);

    boolean createAuthorizationTemp(AuthorizationTempVO authTemp);

    boolean modifyAuthorizationTemp(AuthorizationTempVO authTemp);

    int deleteAuthTemp(String atrzTempSqn);
}
