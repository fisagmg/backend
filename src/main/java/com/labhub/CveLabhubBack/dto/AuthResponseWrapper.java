package com.labhub.CveLabhubBack.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponseWrapper<T> {
    private String status; // "SUCCESS" or "ERROR"
    private T data;        // 성공이면 채움, 실패면 null
    private AuthError error; // 실패면 채움, 성공이면 null

    public static <T> AuthResponseWrapper<T> success(T data) {
        return AuthResponseWrapper.<T>builder()
                .status("SUCCESS")
                .data(data)
                .error(null)
                .build();
    }

    public static <T> AuthResponseWrapper<T> error(int statusCode, String message) {
        return AuthResponseWrapper.<T>builder()
                .status("ERROR")
                .data(null)
                .error(AuthError.builder()
                        .statusCode(statusCode)
                        .message(message)
                        .build())
                .build();
    }
}
