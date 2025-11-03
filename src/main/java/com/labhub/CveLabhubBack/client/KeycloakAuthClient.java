package com.labhub.CveLabhubBack.client;

import com.labhub.CveLabhubBack.dto.KeycloakTokenResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "keycloak-auth-client",
        url = "${keycloak.token-endpoint-base}" // ex: http://localhost:8080/realms/dev-realm
)
public interface KeycloakAuthClient {

    @PostMapping(
            value = "/protocol/openid-connect/token",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    KeycloakTokenResponse getToken(
            @RequestParam("grant_type") String grantType,
            @RequestParam("client_id") String clientId,
            @RequestParam(value = "client_secret", required = false) String clientSecret,
            @RequestParam("username") String username,
            @RequestParam("password") String password
    );
}
