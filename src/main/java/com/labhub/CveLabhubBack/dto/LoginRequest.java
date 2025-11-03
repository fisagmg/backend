package com.labhub.CveLabhubBack.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class LoginRequest {

    @Email(message = "올바른 이메일 형식이 아닙니다.")
    @NotBlank(message = "email은 필수 값입니다.")
    private String email;

    @NotBlank(message = "password는 필수 값입니다.")
    private String password;
}
