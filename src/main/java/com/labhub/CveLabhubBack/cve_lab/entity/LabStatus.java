package com.labhub.CveLabhubBack.cve_lab.entity;

public enum LabStatus {
    ACTIVE,     // VM 생성 완료, 사용자가 실습 중, 보고서 작성 가능
    TERMINATED; // VM 종료됨 (실습 완료 또는 VM만 종료), done_cve 테이블로 완료 여부 구분

    public static LabStatus from(String value) {
        if (value == null) {
            return ACTIVE;
        }
        return switch (value.toLowerCase()) {
            case "active" -> ACTIVE;
            case "terminated", "completed", "cancelled", "canceled", "created" -> TERMINATED;
            default -> ACTIVE;
        };
    }
}
