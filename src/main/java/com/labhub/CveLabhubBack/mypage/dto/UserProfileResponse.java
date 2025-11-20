package com.labhub.CveLabhubBack.mypage.dto;

import com.labhub.CveLabhubBack.auth.entity.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String fullName; // firstName + lastName 조합

    public static UserProfileResponse fromEntity(UserEntity user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .fullName(user.getFirstName() + " " + user.getLastName())
                .build();
    }
}

