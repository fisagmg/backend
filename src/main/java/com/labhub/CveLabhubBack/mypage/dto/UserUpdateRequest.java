package com.labhub.CveLabhubBack.mypage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {
    @NotBlank(message = "이름은 필수입니다.")
    @Size(max = 100, message = "이름은 100자 이하여야 합니다.")
    private String firstName;

    @NotBlank(message = "성은 필수입니다.")
    @Size(max = 100, message = "성은 100자 이하여야 합니다.")
    private String lastName;

    @Size(max = 30, message = "전화번호는 30자 이하여야 합니다.")
    private String phone;
}

