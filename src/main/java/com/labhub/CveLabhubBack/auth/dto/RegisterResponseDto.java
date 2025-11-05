package com.labhub.CveLabhubBack.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RegisterResponseDto {
    private String userId; // Keycloak userId
    private String email;
}
