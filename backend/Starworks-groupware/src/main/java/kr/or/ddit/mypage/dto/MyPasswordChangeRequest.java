package kr.or.ddit.mypage.dto;

import lombok.Data;

@Data
public class MyPasswordChangeRequest {

    private String currentPassword;
    private String newPassword;
}
