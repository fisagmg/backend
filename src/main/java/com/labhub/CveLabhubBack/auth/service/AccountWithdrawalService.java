package com.labhub.CveLabhubBack.auth.service;

import com.labhub.CveLabhubBack.auth.Repository.UserRepository;
import com.labhub.CveLabhubBack.auth.entity.UserEntity;
import com.labhub.CveLabhubBack.cve_lab.client.RunnerClient;
import com.labhub.CveLabhubBack.cve_lab.dto.lab_run.RunRequest;
import com.labhub.CveLabhubBack.cve_lab.entity.Lab;
import com.labhub.CveLabhubBack.cve_lab.entity.LabStatus;
import com.labhub.CveLabhubBack.cve_lab.repository.LabRepository;
import com.labhub.CveLabhubBack.cve_lab.service.GuacamoleService;
import com.labhub.CveLabhubBack.mypage.repository.DoneCveRepository;
import com.labhub.CveLabhubBack.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountWithdrawalService {

    private final UserRepository userRepository;
    private final KeycloakAdminService keycloakAdminService;
    private final LabRepository labRepository;
    private final DoneCveRepository doneCveRepository;
    private final ReportRepository reportRepository;
    private final RunnerClient runnerClient;
    private final GuacamoleService guacamoleService;

    @Transactional
    public void withdraw(Jwt jwt) {
        if (jwt == null) {
            throw new IllegalArgumentException("인증 정보가 필요합니다.");
        }

        String email = extractEmail(jwt)
                .orElseThrow(() -> new IllegalArgumentException("JWT에서 이메일을 확인할 수 없습니다."));

        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));

        Long userId = user.getId();
        String kcUserId = user.getKcUserId();

        log.info("[WITHDRAW] 사용자 탈퇴 진행: userId={}, kcUserId={}, email={}", userId, kcUserId, email);

        cleanupLabs(user);
        cleanupDoneCves(userId);
        cleanupReports(userId);

        userRepository.delete(user);
        log.info("[WITHDRAW] 사용자 DB 삭제 완료: userId={}", userId);

        keycloakAdminService.deleteUser(kcUserId);
        log.info("[WITHDRAW] Keycloak 사용자 삭제 완료: kcUserId={}", kcUserId);
    }

    private Optional<String> extractEmail(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        if (email != null && !email.isBlank()) {
            return Optional.of(email);
        }

        String username = jwt.getClaimAsString("preferred_username");
        if (username != null && !username.isBlank()) {
            return Optional.of(username);
        }
        return Optional.empty();
    }

    private void cleanupLabs(UserEntity user) {
        List<Lab> labs = labRepository.findAllByUser_Id(user.getId());
        if (labs.isEmpty()) {
            log.info("[WITHDRAW] 사용자 Lab 데이터 없음: userId={}", user.getId());
            return;
        }

        String userIdAsString = String.valueOf(user.getId());
        for (Lab lab : labs) {
            if (lab.getStatus() == LabStatus.ACTIVE) {
                try {
                    runnerClient.destroy(new RunRequest(lab.getUuid(), lab.getCveName(), userIdAsString));
                    log.info("[WITHDRAW] Runner destroy 호출 완료: labUuid={}", lab.getUuid());
                } catch (Exception e) {
                    log.warn("[WITHDRAW] Runner destroy 실패: labUuid={}, err={}", lab.getUuid(), e.getMessage());
                }
            }

            try {
                guacamoleService.deleteConnectionForLab(lab);
            } catch (Exception e) {
                log.warn("[WITHDRAW] Guacamole 연결 삭제 실패: labUuid={}, err={}", lab.getUuid(), e.getMessage());
            }
        }

        labRepository.deleteAll(labs);
        log.info("[WITHDRAW] Lab 데이터 삭제 완료: userId={}, count={}", user.getId(), labs.size());
    }

    private void cleanupDoneCves(Long userId) {
        long deletedCount = doneCveRepository.deleteAllByUserId(userId);
        log.info("[WITHDRAW] 완료된 CVE 데이터 삭제 완료: userId={}, count={}", userId, deletedCount);
    }

    private void cleanupReports(Long userId) {
        long deletedCount = reportRepository.deleteAllByUserId(userId);
        log.info("[WITHDRAW] 보고서 데이터 삭제 완료: userId={}, count={}", userId, deletedCount);
    }
}

