package kr.or.ddit.mypage.service;

import org.springframework.web.multipart.MultipartFile;

import kr.or.ddit.mypage.dto.MyPasswordChangeRequest;
import kr.or.ddit.vo.UsersVO;

public interface MyPageService {

    UsersVO updateUserInfo(String userId, UsersVO updatedUser, MultipartFile fileList);

    void changePassword(String userId, MyPasswordChangeRequest request);
}
