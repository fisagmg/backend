package com.labhub.CveLabhubBack.auth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequestDto {
    // 프론트에서 보내줄 필드 (필요한 것만)
    private String email;     // username 과 동일하게 쓸 예정
    private String password;  // 초기 비밀번호
    private String firstName;
    private String lastName;
    private String phone;     // attributes.phone 로 저장
}
