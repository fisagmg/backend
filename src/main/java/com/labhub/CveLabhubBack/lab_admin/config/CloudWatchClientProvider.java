package com.labhub.CveLabhubBack.lab_admin.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClientBuilder;
import software.amazon.awssdk.utils.StringUtils;

import jakarta.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CloudWatchClient 생성을 중앙집중화하여 다중 리전 실습 환경을 지원한다.
 */
@Slf4j
@Component
public class CloudWatchClientProvider {

    private final Map<Region, CloudWatchClient> clients = new ConcurrentHashMap<>();
    private final Region defaultRegion;
    private final String accessKey;
    private final String secretKey;
    private final String sessionToken;

    public CloudWatchClientProvider(
            @Value("${aws.cloudwatch.default-region:ap-northeast-2}") String defaultRegion,
            @Value("${aws.cloudwatch.access-key:}") String accessKey,
            @Value("${aws.cloudwatch.secret-key:}") String secretKey,
            @Value("${aws.cloudwatch.session-token:}") String sessionToken
    ) {
        this.defaultRegion = Region.of(defaultRegion);
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.sessionToken = sessionToken;

        if (log.isDebugEnabled()) {
            log.debug("CloudWatch credentials configured - region={}, accessKey={}, secretKey={}",
                    this.defaultRegion,
                    maskForLog(this.accessKey),
                    maskForLog(this.secretKey));
        }
    }

    /**
     * 주어진 리전에 대한 CloudWatchClient를 반환한다.
     * region 값이 비어있는 경우 기본 리전을 사용한다.
     */
    public CloudWatchClient getClient(String regionName) {
        Region resolvedRegion = resolveRegion(regionName);
        return clients.computeIfAbsent(resolvedRegion, this::createClient);
    }

    private CloudWatchClient createClient(Region region) {
        try {
            CloudWatchClientBuilder builder = CloudWatchClient.builder()
                    .region(region);

            AwsCredentialsProvider credentialsProvider = buildCredentialsProvider();
            if (credentialsProvider != null) {
                builder = builder.credentialsProvider(credentialsProvider);
            }

            return builder.build();
        } catch (AwsServiceException ex) {
            throw new IllegalStateException("CloudWatchClient 생성 실패 (region=" + region + ")", ex);
        }
    }

    private Region resolveRegion(String regionName) {
        if (StringUtils.isBlank(regionName)) {
            return defaultRegion;
        }
        return Region.of(regionName);
    }

    @PreDestroy
    public void shutdown() {
        clients.values().forEach(CloudWatchClient::close);
        clients.clear();
    }

    private AwsCredentialsProvider buildCredentialsProvider() {
        if (StringUtils.isBlank(accessKey) || StringUtils.isBlank(secretKey)) {
            return null;
        }
        if (StringUtils.isNotBlank(sessionToken)) {
            return StaticCredentialsProvider.create(
                    AwsSessionCredentials.create(accessKey, secretKey, sessionToken)
            );
        }
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey)
        );
    }

    private String maskForLog(String value) {
        if (StringUtils.isBlank(value)) {
            return "(none)";
        }
        int visible = Math.min(4, value.length());
        return value.substring(0, visible) + "****";
    }
}


