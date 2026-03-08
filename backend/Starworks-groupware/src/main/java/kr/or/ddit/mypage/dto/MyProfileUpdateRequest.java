package kr.or.ddit.mypage.dto;

import org.springframework.web.multipart.MultipartFile;

import lombok.Data;

@Data
public class MyProfileUpdateRequest {

    private String userNm;
    private String userEmail;
    private String userTelno;
    private String extTel;
    private MultipartFile profileImage;
}
