package com.labhub.CveLabhubBack.cve_lab.entity;

public enum LabStatus {
    CREATED,    // 실습 시작 버튼 눌렀을 때, VM은 아직 생성되지 않았거나 생성 중
    ACTIVE,     // VM 생성 완료, 사용자가 실습 중, 보고서 작성 가능
    TERMINATED, // VM은 종료됐지만 실습 자체는 유지되는 상태
    COMPLETED,  // 사용자가 "실습 완료" 버튼 클릭, VM 자동 종료, 마이페이지에 표시
    CANCELLED;   // 실습 중단/포기, VM 바로 종료, 마이페이지 기록 없음

    public static LabStatus from(String value) {
        if (value == null) {
            return CREATED;
        }
        return switch (value.toLowerCase()) {
            case "active" -> ACTIVE;
            case "terminated" -> TERMINATED;
            case "completed" -> COMPLETED;
            case "cancelled", "canceled" -> CANCELLED;
            case "created" -> CREATED;
            default -> CREATED;
        };
    }
}

