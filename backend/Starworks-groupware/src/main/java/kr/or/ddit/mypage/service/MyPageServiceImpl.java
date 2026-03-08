package kr.or.ddit.mypage.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import kr.or.ddit.comm.file.FileFolderType;
import kr.or.ddit.comm.file.service.impl.FileUploadServiceImpl;
import kr.or.ddit.mypage.dto.MyPasswordChangeRequest;
import kr.or.ddit.mybatis.mapper.MyPageMapper;
import kr.or.ddit.mybatis.mapper.UsersMapper;
import kr.or.ddit.vo.UsersVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MyPageServiceImpl implements MyPageService {

    private final MyPageMapper mapper;
    private final UsersMapper usersMapper;
    private final FileUploadServiceImpl fileService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UsersVO updateUserInfo(String userId, UsersVO updatedUser, MultipartFile fileList) {
        updatedUser.setUserId(userId);

        if (fileList != null && !fileList.isEmpty()) {
            updatedUser.setFileList(List.of(fileList));
            fileService.saveFileS3(updatedUser, FileFolderType.PROFILE.toString());
            updatedUser.setUserImgFileId(updatedUser.getUserImgFileId());
        }

        int updateRows = mapper.updateUserInfo(updatedUser);
        log.info("사용자 정보 업데이트 완료 (수정된 행 수: {})", updateRows);
        return usersMapper.selectUser(userId);
    }

    @Override
    public void changePassword(String userId, MyPasswordChangeRequest request) {
        UsersVO existingUser = usersMapper.selectUser(userId);
        if (existingUser == null) {
            throw new IllegalArgumentException("사용자 정보를 찾을 수 없습니다.");
        }

        if (request == null
            || request.getCurrentPassword() == null
            || request.getNewPassword() == null
            || request.getNewPassword().trim().length() < 4) {
            throw new IllegalArgumentException("비밀번호 입력값이 올바르지 않습니다.");
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), existingUser.getUserPswd())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        mapper.updateUserPassword(userId, passwordEncoder.encode(request.getNewPassword().trim()));
    }
}
