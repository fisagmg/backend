package com.labhub.CveLabhubBack.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthError {
    private int statusCode;
    private String message;
}
