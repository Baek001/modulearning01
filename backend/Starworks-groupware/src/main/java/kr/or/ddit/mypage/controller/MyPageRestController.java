package kr.or.ddit.mypage.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kr.or.ddit.mypage.dto.MyPasswordChangeRequest;
import kr.or.ddit.mypage.dto.MyProfileUpdateRequest;
import kr.or.ddit.mypage.service.MyPageService;
import kr.or.ddit.security.CustomUserDetails;
import kr.or.ddit.users.service.UsersService;
import kr.or.ddit.vo.UsersVO;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/rest/mypage")
@RequiredArgsConstructor
public class MyPageRestController {

    private final MyPageService service;
    private final UsersService usersService;

    @GetMapping
    public UsersVO getMyInfo(@AuthenticationPrincipal CustomUserDetails user) {
        return usersService.readUser(user.getUsername());
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateMyInfo(
        @AuthenticationPrincipal CustomUserDetails user,
        @ModelAttribute MyProfileUpdateRequest request
    ) {
        UsersVO vo = new UsersVO();
        vo.setUserNm(request.getUserNm());
        vo.setUserEmail(request.getUserEmail());
        vo.setUserTelno(request.getUserTelno());
        vo.setExtTel(request.getExtTel());

        return ResponseEntity.ok(service.updateUserInfo(user.getUsername(), vo, request.getProfileImage()));
    }

    @PutMapping("/password")
    public ResponseEntity<?> updatePassword(
        @AuthenticationPrincipal CustomUserDetails user,
        @RequestBody MyPasswordChangeRequest request
    ) {
        service.changePassword(user.getUsername(), request);
        return ResponseEntity.ok().build();
    }
}
