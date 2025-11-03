package com.labhub.CveLabhubBack.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthSuccessData {
    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
    private String tokenType;
}
