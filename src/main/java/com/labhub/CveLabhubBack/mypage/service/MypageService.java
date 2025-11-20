package com.labhub.CveLabhubBack.mypage.service;

import com.labhub.CveLabhubBack.auth.Repository.UserRepository;
import com.labhub.CveLabhubBack.auth.service.KeycloakAdminService;
import com.labhub.CveLabhubBack.auth.entity.UserEntity;
import com.labhub.CveLabhubBack.mypage.dto.CompletedCveResponse;
import com.labhub.CveLabhubBack.mypage.dto.PasswordChangeRequest;
import com.labhub.CveLabhubBack.mypage.dto.UserProfileResponse;
import com.labhub.CveLabhubBack.mypage.dto.UserUpdateRequest;
import com.labhub.CveLabhubBack.mypage.entity.DoneCve;
import com.labhub.CveLabhubBack.mypage.repository.DoneCveRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MypageService {

    private final UserRepository userRepository;
    private final KeycloakAdminService keycloakAdminService;
    private final DoneCveRepository doneCveRepository;

    /**
     * 현재 로그인한 사용자 정보 조회
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile(Jwt jwt) {
        String email = getEmailFromJwt(jwt);
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));

        log.info("사용자 정보 조회: userId={}, email={}", user.getId(), user.getEmail());
        return UserProfileResponse.fromEntity(user);
    }

    /**
     * 사용자 정보 수정
     */
    @Transactional
    public UserProfileResponse updateUserProfile(Jwt jwt, UserUpdateRequest request) {
        String email = getEmailFromJwt(jwt);
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));

        // DB 업데이트
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        userRepository.save(user);

        // Keycloak 업데이트 (email은 필수 필드이므로 기존 이메일을 함께 전달)
        try {
            keycloakAdminService.updateUser(
                    user.getKcUserId(),
                    user.getEmail(),  // 기존 이메일 포함 (Keycloak 필수 필드)
                    request.getFirstName(),
                    request.getLastName(),
                    request.getPhone()
            );
            log.info("Keycloak 사용자 정보 업데이트 성공: userId={}", user.getKcUserId());
        } catch (Exception e) {
            log.error("Keycloak 사용자 정보 업데이트 실패: userId={}, error={}", user.getKcUserId(), e.getMessage());
            // DB는 업데이트되었지만 Keycloak 실패 시 예외 발생
            throw new RuntimeException("사용자 정보 업데이트 중 오류가 발생했습니다: " + e.getMessage(), e);
        }

        log.info("사용자 정보 수정 완료: userId={}, email={}", user.getId(), user.getEmail());
        return UserProfileResponse.fromEntity(user);
    }

    /**
     * 비밀번호 변경
     */
    @Transactional
    public void changePassword(Jwt jwt, PasswordChangeRequest request) {
        String email = getEmailFromJwt(jwt);
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));

        // 새 비밀번호와 확인 비밀번호 일치 확인
        if (!request.isPasswordMatch()) {
            throw new IllegalArgumentException("새 비밀번호와 확인 비밀번호가 일치하지 않습니다.");
        }

        // 현재 비밀번호 검증
        boolean isPasswordValid = keycloakAdminService.verifyPassword(email, request.getCurrentPassword());
        if (!isPasswordValid) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        // Keycloak 비밀번호 변경
        try {
            keycloakAdminService.changePassword(user.getKcUserId(), request.getNewPassword());
            log.info("비밀번호 변경 성공: userId={}, email={}", user.getId(), email);
        } catch (Exception e) {
            log.error("비밀번호 변경 실패: userId={}, error={}", user.getKcUserId(), e.getMessage());
            throw new RuntimeException("비밀번호 변경 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 완료된 CVE 목록 조회
     */
    @Transactional(readOnly = true)
    public List<CompletedCveResponse> getCompletedCves(Jwt jwt) {
        String email = getEmailFromJwt(jwt);
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));

        // Fetch join을 사용하여 CVE 정보도 함께 조회 (N+1 문제 방지)
        List<DoneCve> doneCves = doneCveRepository.findByUserIdWithCveOrderByFinishedAtDesc(user.getId());

        log.info("완료된 CVE 목록 조회: userId={}, count={}", user.getId(), doneCves.size());

        return doneCves.stream()
                .map(CompletedCveResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * JWT에서 이메일 추출
     */
    private String getEmailFromJwt(Jwt jwt) {
        if (jwt == null) {
            throw new IllegalStateException("인증 정보가 없습니다.");
        }
        
        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            email = jwt.getClaimAsString("preferred_username");
        }
        
        if (email == null || email.isBlank()) {
            throw new IllegalStateException("JWT에서 이메일을 찾을 수 없습니다.");
        }
        
        return email;
    }
}

