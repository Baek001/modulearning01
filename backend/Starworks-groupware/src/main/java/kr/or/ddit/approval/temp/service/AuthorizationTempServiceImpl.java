package kr.or.ddit.approval.temp.service;

import java.util.List;
import java.util.Objects;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import kr.or.ddit.comm.exception.EntityNotFoundException;
import kr.or.ddit.comm.file.FileFolderType;
import kr.or.ddit.comm.file.service.impl.FileUploadServiceImpl;
import kr.or.ddit.mybatis.mapper.AuthorizationTempMapper;
import kr.or.ddit.vo.AuthorizationTempVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthorizationTempServiceImpl implements AuthorizationTempService {

    private final AuthorizationTempMapper mapper;
    private final FileUploadServiceImpl fileService;

    @Override
    public List<AuthorizationTempVO> readAuthTempList() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String loginId = auth.getName();

        return mapper.selectAuthTempList(loginId);
    }

    @Override
    public AuthorizationTempVO readAuthTemp(String atrzTempSqn) {
        AuthorizationTempVO authTemp = mapper.selectAuthTemp(atrzTempSqn);
        if (authTemp == null) {
            throw new EntityNotFoundException(atrzTempSqn);
        }
        return authTemp;
    }

    @Override
    public boolean createAuthorizationTemp(AuthorizationTempVO authTemp) {
        if (authTemp.getFileList() != null && !authTemp.getFileList().isEmpty()) {
            fileService.saveFileS3(authTemp, FileFolderType.APPROVAL.toString());
        }

        int result = mapper.insertAuthorizationTemp(authTemp);
        log.info("임시저장 문서 insert 완료 => tempSqn={}", authTemp.getAtrzTempSqn());
        return result > 0;
    }

    @Override
    public boolean modifyAuthorizationTemp(AuthorizationTempVO authTemp) {
        AuthorizationTempVO existing = readAuthTemp(authTemp.getAtrzTempSqn());
        if (authTemp.getFileList() != null && !authTemp.getFileList().isEmpty()) {
            authTemp.setAtrzFileId(null);
            fileService.saveFileS3(authTemp, FileFolderType.APPROVAL.toString());
        } else if (authTemp.getAtrzFileId() == null) {
            authTemp.setAtrzFileId(existing.getAtrzFileId());
        }

        authTemp.setAtrzUserId(Objects.requireNonNullElse(authTemp.getAtrzUserId(), existing.getAtrzUserId()));
        int result = mapper.updateAuthorizationTemp(authTemp);
        return result > 0;
    }

    @Override
    public int deleteAuthTemp(String atrzTempSqn) {
        return mapper.deleteAuthTemp(atrzTempSqn);
    }
}
