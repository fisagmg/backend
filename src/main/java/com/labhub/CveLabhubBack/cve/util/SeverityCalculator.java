package com.labhub.CveLabhubBack.cve.util;

/**
 * CVSS Score를 기반으로 Severity(심각도)를 계산하는 유틸리티 클래스
 */
public class SeverityCalculator {

    /**
     * CVSS Score를 Severity 문자열로 변환
     * 
     * 매핑 규칙:
     * - Critical: 9.0 ~ 10.0
     * - High: 7.0 ~ 8.9
     * - Medium: 4.0 ~ 6.9 (4.0 미만도 Medium으로 처리)
     *
     * @param cvssScore CVSS 점수 (0.0 ~ 10.0)
     * @return Severity 문자열 ("Critical", "High", "Medium")
     */
    public static String toSeverity(float cvssScore) {
        if (cvssScore >= 9.0f) {
            return "Critical";
        } else if (cvssScore >= 7.0f) {
            return "High";
        } else {
            return "Medium";
        }
    }

    /**
     * Severity 타입을 정의하는 열거형
     */
    public enum Severity {
        CRITICAL("Critical", 9.0f, 10.0f),
        HIGH("High", 7.0f, 8.9f),
        MEDIUM("Medium", 0.0f, 6.9f);

        private final String displayName;
        private final float minScore;
        private final float maxScore;

        Severity(String displayName, float minScore, float maxScore) {
            this.displayName = displayName;
            this.minScore = minScore;
            this.maxScore = maxScore;
        }

        public String getDisplayName() {
            return displayName;
        }

        public float getMinScore() {
            return minScore;
        }

        public float getMaxScore() {
            return maxScore;
        }

        /**
         * CVSS Score가 해당 Severity 범위에 속하는지 확인
         */
        public boolean isInRange(float cvssScore) {
            return cvssScore >= minScore && cvssScore <= maxScore;
        }

        /**
         * CVSS Score로부터 Severity를 찾음
         */
        public static Severity fromCvssScore(float cvssScore) {
            if (cvssScore >= 9.0f) {
                return CRITICAL;
            } else if (cvssScore >= 7.0f) {
                return HIGH;
            } else {
                return MEDIUM;
            }
        }
    }
}



